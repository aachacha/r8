// Copyright (c) 2016, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.code;

import com.android.tools.r8.ir.code.NumericType;
import com.android.tools.r8.ir.conversion.IRBuilder;

public class MulLong extends Format23x {

  public static final int OPCODE = 0x9d;
  public static final String NAME = "MulLong";
  public static final String SMALI_NAME = "mul-long";

  MulLong(int high, BytecodeStream stream) {
    super(high, stream);
  }

  public MulLong(int dest, int left, int right) {
    super(dest, left, right);
    // The art x86 backend had a bug that made it fail on "mul r0, r1, r0" instructions where
    // the second src register and the dst register is the same (but the first src register is
    // different). Therefore, we have to avoid generating that pattern. The bug was fixed for
    // Android M: https://android-review.googlesource.com/#/c/114932/
    assert dest != right || dest == left;
  }

  public String getName() {
    return NAME;
  }

  public String getSmaliName() {
    return SMALI_NAME;
  }

  public int getOpcode() {
    return OPCODE;
  }

  @Override
  public void buildIR(IRBuilder builder) {
    builder.addMul(NumericType.LONG, AA, BB, CC);
  }
}
