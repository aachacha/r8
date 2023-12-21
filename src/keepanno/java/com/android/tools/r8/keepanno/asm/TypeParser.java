// Copyright (c) 2023, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.keepanno.asm;

import com.android.tools.r8.keepanno.asm.ClassNameParser.ClassNameProperty;
import com.android.tools.r8.keepanno.asm.TypeParser.TypeProperty;
import com.android.tools.r8.keepanno.ast.AnnotationConstants.TypePattern;
import com.android.tools.r8.keepanno.ast.KeepTypePattern;
import com.android.tools.r8.keepanno.ast.ParsingContext;
import com.android.tools.r8.keepanno.ast.ParsingContext.AnnotationParsingContext;
import com.android.tools.r8.keepanno.utils.Unimplemented;
import java.util.function.Consumer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

public class TypeParser extends PropertyParserBase<KeepTypePattern, TypeProperty> {

  public TypeParser(ParsingContext parsingContext) {
    super(parsingContext);
  }

  public enum TypeProperty {
    TYPE_PATTERN,
    TYPE_NAME,
    TYPE_CONSTANT,
    CLASS_NAME_PATTERN
  }

  @Override
  public boolean tryProperty(
      TypeProperty property, String name, Object value, Consumer<KeepTypePattern> setValue) {
    switch (property) {
      case TYPE_NAME:
        setValue.accept(KeepEdgeReaderUtils.typePatternFromString((String) value));
        return true;
      case TYPE_CONSTANT:
        setValue.accept(KeepTypePattern.fromDescriptor(((Type) value).getDescriptor()));
        return true;
      default:
        return false;
    }
  }

  @Override
  public AnnotationVisitor tryPropertyAnnotation(
      TypeProperty property, String name, String descriptor, Consumer<KeepTypePattern> setValue) {
    switch (property) {
      case TYPE_PATTERN:
        {
          AnnotationParsingContext parsingContext =
              new AnnotationParsingContext(getParsingContext(), descriptor);
          TypeParser typeParser = new TypeParser(parsingContext);
          typeParser.setKind(kind());
          typeParser.setProperty(TypePattern.name, TypeProperty.TYPE_NAME);
          typeParser.setProperty(TypePattern.constant, TypeProperty.TYPE_CONSTANT);
          typeParser.setProperty(TypePattern.classNamePattern, TypeProperty.CLASS_NAME_PATTERN);
          return new ParserVisitor(
              parsingContext,
              descriptor,
              typeParser,
              () -> setValue.accept(typeParser.getValueOrDefault(KeepTypePattern.any())));
        }
      case CLASS_NAME_PATTERN:
        {
          ClassNameParser parser = new ClassNameParser(getParsingContext());
          parser.setKind(kind());
          return parser.tryPropertyAnnotation(
              ClassNameProperty.PATTERN,
              name,
              descriptor,
              classNamePattern -> {
                if (classNamePattern.isExact()) {
                  setValue.accept(
                      KeepTypePattern.fromDescriptor(classNamePattern.getExactDescriptor()));
                } else {
                  // TODO(b/248408342): Extend the AST type patterns.
                  throw new Unimplemented("Non-exact class patterns are not implemented yet");
                }
              });
        }
      default:
        return null;
    }
  }
}
