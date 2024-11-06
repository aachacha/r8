// Copyright (c) 2024, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.metadata;

import com.android.tools.r8.keepanno.annotations.KeepForApi;

@KeepForApi
public interface R8KeepAttributesMetadata {

  boolean isAnnotationDefaultKept();

  boolean isEnclosingMethodKept();

  boolean isExceptionsKept();

  boolean isInnerClassesKept();

  boolean isLocalVariableTableKept();

  boolean isLocalVariableTypeTableKept();

  boolean isMethodParametersKept();

  boolean isPermittedSubclassesKept();

  boolean isRuntimeInvisibleAnnotationsKept();

  boolean isRuntimeInvisibleParameterAnnotationsKept();

  boolean isRuntimeInvisibleTypeAnnotationsKept();

  boolean isRuntimeVisibleAnnotationsKept();

  boolean isRuntimeVisibleParameterAnnotationsKept();

  boolean isRuntimeVisibleTypeAnnotationsKept();

  boolean isSignatureKept();

  boolean isSourceDebugExtensionKept();

  boolean isSourceDirKept();

  boolean isSourceFileKept();

  boolean isStackMapTableKept();
}