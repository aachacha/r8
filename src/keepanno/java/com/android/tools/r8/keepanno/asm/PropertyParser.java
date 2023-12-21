// Copyright (c) 2023, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.keepanno.asm;

import java.util.function.Consumer;
import org.objectweb.asm.AnnotationVisitor;

public interface PropertyParser<T, P> {

  String kind();

  void setProperty(String name, P property);

  boolean isDeclared();

  default boolean isDefault() {
    return !isDeclared();
  }

  T getValue();

  T tryParse(String name, Object value);

  default boolean tryParse(String name, Object value, Consumer<T> setValue) {
    T result = tryParse(name, value);
    if (result != null) {
      setValue.accept(result);
      return true;
    }
    return false;
  }

  boolean tryParseEnum(String name, String descriptor, String value, Consumer<T> setValue);

  AnnotationVisitor tryParseArray(String name, Consumer<T> setValue);

  AnnotationVisitor tryParseAnnotation(String name, String descriptor, Consumer<T> setValue);
}
