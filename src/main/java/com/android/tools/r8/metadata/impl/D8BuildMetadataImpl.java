// Copyright (c) 2024, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.metadata.impl;

import com.android.tools.r8.keepanno.annotations.AnnotationPattern;
import com.android.tools.r8.keepanno.annotations.FieldAccessFlags;
import com.android.tools.r8.keepanno.annotations.KeepConstraint;
import com.android.tools.r8.keepanno.annotations.KeepItemKind;
import com.android.tools.r8.keepanno.annotations.UsedByReflection;
import com.android.tools.r8.metadata.D8BuildMetadata;
import com.android.tools.r8.metadata.D8Options;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@UsedByReflection(
    description = "Keep and preserve @SerializedName for correct (de)serialization",
    constraints = {KeepConstraint.LOOKUP},
    constrainAnnotations = @AnnotationPattern(constant = SerializedName.class),
    kind = KeepItemKind.CLASS_AND_FIELDS,
    fieldAccess = {FieldAccessFlags.PRIVATE},
    fieldAnnotatedByClassConstant = SerializedName.class)
public class D8BuildMetadataImpl implements D8BuildMetadata {

  @Expose
  @SerializedName("options")
  private final D8Options options;

  @Expose
  @SerializedName("version")
  private final String version;

  public D8BuildMetadataImpl(D8Options options, String version) {
    this.options = options;
    this.version = version;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public D8Options getOptions() {
    return options;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String toJson() {
    return new Gson().toJson(this);
  }

  public static class Builder {

    private D8Options options;
    private String version;

    public Builder setOptions(D8Options options) {
      this.options = options;
      return this;
    }

    public Builder setVersion(String version) {
      this.version = version;
      return this;
    }

    public D8BuildMetadataImpl build() {
      return new D8BuildMetadataImpl(options, version);
    }
  }
}