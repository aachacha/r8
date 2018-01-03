// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.apiusagesample;

import com.android.tools.r8.ArchiveClassFileProvider;
import com.android.tools.r8.CompilationFailedException;
import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.DexFilePerClassFileConsumer;
import com.android.tools.r8.DexIndexedConsumer;
import com.android.tools.r8.DiagnosticsHandler;
import com.android.tools.r8.origin.ArchiveEntryOrigin;
import com.android.tools.r8.origin.Origin;
import com.android.tools.r8.origin.PathOrigin;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class D8ApiUsageSample {

  private static final Origin origin =
      new Origin(Origin.root()) {
        @Override
        public String part() {
          return "D8ApiUsageSample";
        }
      };

  private static final DiagnosticsHandler handler = new D8DiagnosticsHandler();

  /**
   * Example invocation:
   *
   * <pre>
   *   java -jar d8-api-uses.jar \
   *     --output path/to/output/dir \
   *     --min-api minApiLevel \
   *     --lib path/to/library.jar \
   *     --classpath path/to/classpath.jar \
   *     path/to/input{1,2,3}.{jar,class}
   * </pre>
   */
  public static void main(String[] args) {
    // Parse arguments with the commandline parser to make use of its API.
    D8Command.Builder cmd = D8Command.parse(args, origin);
    CompilationMode mode = cmd.getMode();
    Path temp = cmd.getOutputPath();
    int minApiLevel = cmd.getMinApiLevel();
    // The Builder API does not provide access to the concrete paths
    // (everything is put into providers) so manually parse them here.
    List<Path> libraries = new ArrayList<>(1);
    List<Path> classpath = new ArrayList<>(args.length);
    List<Path> inputs = new ArrayList<>(args.length);
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--lib")) {
        libraries.add(Paths.get(args[++i]));
      } else if (args[i].equals("--classpath")) {
        classpath.add(Paths.get(args[++i]));
      } else if (isArchive(args[i]) || isClassFile(args[i])) {
        inputs.add(Paths.get(args[i]));
      }
    }
    if (!Files.exists(temp) || !Files.isDirectory(temp)) {
      throw new RuntimeException("Must supply a temp/output directory");
    }
    if (inputs.isEmpty()) {
      throw new RuntimeException("Must supply program inputs");
    }
    if (classpath.isEmpty()) {
      throw new RuntimeException("Must supply classpath inputs");
    }
    if (libraries.isEmpty()) {
      throw new RuntimeException("Must supply library inputs");
    }

    useProgramFileBuilder(CompilationMode.DEBUG, minApiLevel, libraries, classpath, inputs);
    useProgramFileBuilder(CompilationMode.RELEASE, minApiLevel, libraries, classpath, inputs);
    useProgramDataBuilder(minApiLevel, libraries, classpath, inputs);
    useLibraryAndClasspathProvider(minApiLevel, libraries, classpath, inputs);
    incrementalCompileAndMerge(minApiLevel, libraries, classpath, inputs);
  }

  // Check API support for compiling Java class-files from the file system.
  private static void useProgramFileBuilder(
      CompilationMode mode,
      int minApiLevel,
      Collection<Path> libraries,
      Collection<Path> classpath,
      Collection<Path> inputs) {
    try {
      D8.run(
          D8Command.builder(handler)
              .setMode(mode)
              .setMinApiLevel(minApiLevel)
              .setProgramConsumer(DexIndexedConsumer.emptyConsumer())
              .addLibraryFiles(libraries)
              .addClasspathFiles(classpath)
              .addProgramFiles(inputs)
              .build());
    } catch (CompilationFailedException e) {
      throw new RuntimeException("Unexpected compilation exceptions", e);
    }
  }

  // Check API support for compiling Java class-files from byte content.
  private static void useProgramDataBuilder(
      int minApiLevel,
      Collection<Path> libraries,
      Collection<Path> classpath,
      Collection<Path> inputs) {
    try {
      D8Command.Builder builder =
          D8Command.builder(handler)
              .setMinApiLevel(minApiLevel)
              .setProgramConsumer(DexIndexedConsumer.emptyConsumer())
              .addLibraryFiles(libraries)
              .addClasspathFiles(classpath);
      for (ClassFileContent classfile : readClassFiles(inputs)) {
        builder.addClassProgramData(classfile.data, classfile.origin);
      }
      D8.run(builder.build());
    } catch (CompilationFailedException e) {
      throw new RuntimeException("Unexpected compilation exceptions", e);
    } catch (IOException e) {
      throw new RuntimeException("Unexpected IO exception", e);
    }
  }

  private static void useLibraryAndClasspathProvider(
      int minApiLevel,
      Collection<Path> libraries,
      Collection<Path> classpath,
      Collection<Path> inputs) {
    try {
      D8Command.Builder builder =
          D8Command.builder(handler)
              .setMinApiLevel(minApiLevel)
              .setProgramConsumer(DexIndexedConsumer.emptyConsumer())
              .addProgramFiles(inputs);
      for (Path library : libraries) {
        builder.addLibraryResourceProvider(new ArchiveClassFileProvider(library));
      }
      for (Path path : classpath) {
        builder.addClasspathResourceProvider(new ArchiveClassFileProvider(path));
      }
      D8.run(builder.build());
    } catch (CompilationFailedException e) {
      throw new RuntimeException("Unexpected compilation exceptions", e);
    } catch (IOException e) {
      throw new RuntimeException("Unexpected IO exception", e);
    }
  }

  private static void incrementalCompileAndMerge(
      int minApiLevel,
      Collection<Path> libraries,
      Collection<Path> classpath,
      Collection<Path> inputs) {
    // Compile and merge via index intermediates.
    mergeIntermediates(
        minApiLevel, compileToIndexedIntermediates(minApiLevel, libraries, classpath, inputs));
    // Compile and merge via per-classfile intermediates.
    mergeIntermediates(
        minApiLevel, compileToPerClassFileIntermediates(minApiLevel, libraries, classpath, inputs));
  }

  private static Collection<byte[]> compileToIndexedIntermediates(
      int minApiLevel,
      Collection<Path> libraries,
      Collection<Path> classpath,
      Collection<Path> inputs) {
    IndexIntermediatesConsumer consumer = new IndexIntermediatesConsumer();
    try {
      D8.run(
          D8Command.builder(handler)
              .setMinApiLevel(minApiLevel)
              .setIntermediate(true)
              .setProgramConsumer(consumer)
              .addClasspathFiles(classpath)
              .addLibraryFiles(libraries)
              .addProgramFiles(inputs)
              .build());
    } catch (CompilationFailedException e) {
      throw new RuntimeException("Unexpected compilation exceptions", e);
    }
    return consumer.bytes;
  }

  private static Collection<byte[]> compileToPerClassFileIntermediates(
      int minApiLevel,
      Collection<Path> libraries,
      Collection<Path> classpath,
      Collection<Path> inputs) {
    PerClassIntermediatesConsumer consumer = new PerClassIntermediatesConsumer();
    try {
      D8.run(
          D8Command.builder(handler)
              .setMinApiLevel(minApiLevel)
              .setProgramConsumer(consumer)
              .addLibraryFiles(libraries)
              .addClasspathFiles(classpath)
              .addProgramFiles(inputs)
              .build());
    } catch (CompilationFailedException e) {
      throw new RuntimeException("Unexpected compilation exceptions", e);
    }
    return consumer.bytes;
  }

  private static void mergeIntermediates(int minApiLevel, Collection<byte[]> intermediates) {
    D8Command.Builder builder =
        D8Command.builder(handler)
            .setMinApiLevel(minApiLevel)
            .setProgramConsumer(DexIndexedConsumer.emptyConsumer());
    for (byte[] intermediate : intermediates) {
      builder.addDexProgramData(intermediate, Origin.unknown());
    }
    try {
      D8.run(builder.build());
    } catch (CompilationFailedException e) {
      throw new RuntimeException("Unexpected merging error", e);
    }
  }

  // Helpers for tests.
  // Some of this reimplements stuff in R8 utils, but that is not public API and we should not
  // rely on it.

  private static List<ClassFileContent> readClassFiles(Collection<Path> files) throws IOException {
    List<ClassFileContent> classfiles = new ArrayList<>();
    for (Path file : files) {
      if (isArchive(file)) {
        Origin zipOrigin = new PathOrigin(file);
        ZipInputStream zip = new ZipInputStream(Files.newInputStream(file));
        ZipEntry entry;
        while (null != (entry = zip.getNextEntry())) {
          if (isClassFile(Paths.get(entry.getName()))) {
            Origin origin = new ArchiveEntryOrigin(entry.getName(), zipOrigin);
            classfiles.add(new ClassFileContent(origin, readBytes(zip)));
          }
        }
      } else if (isClassFile(file)) {
        classfiles.add(new ClassFileContent(new PathOrigin(file), Files.readAllBytes(file)));
      }
    }
    return classfiles;
  }

  private static byte[] readBytes(InputStream stream) throws IOException {
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[0xffff];
      for (int length; (length = stream.read(buffer)) != -1; ) {
        bytes.write(buffer, 0, length);
      }
      return bytes.toByteArray();
    }
  }

  private static boolean isClassFile(Path file) {
    return isClassFile(file.toString());
  }

  private static boolean isClassFile(String file) {
    file = file.toLowerCase();
    return file.endsWith(".class");
  }

  private static boolean isArchive(Path file) {
    return isArchive(file.toString());
  }

  private static boolean isArchive(String file) {
    file = file.toLowerCase();
    return file.endsWith(".zip") || file.endsWith(".jar");
  }

  private static class ClassFileContent {
    final Origin origin;
    final byte[] data;

    public ClassFileContent(Origin origin, byte[] data) {
      this.origin = origin;
      this.data = data;
    }
  }

  private static class IndexIntermediatesConsumer implements DexIndexedConsumer {

    List<byte[]> bytes = new ArrayList<>();

    @Override
    public synchronized void accept(
        int fileIndex, byte[] data, Set<String> descriptors, DiagnosticsHandler handler) {
      bytes.add(data);
    }

    @Override
    public void finished(DiagnosticsHandler handler) {}
  }

  private static class PerClassIntermediatesConsumer implements DexFilePerClassFileConsumer {

    List<byte[]> bytes = new ArrayList<>();

    @Override
    public synchronized void accept(
        String primaryClassDescriptor,
        byte[] data,
        Set<String> descriptors,
        DiagnosticsHandler handler) {
      bytes.add(data);
    }

    @Override
    public void finished(DiagnosticsHandler handler) {}
  }
}
