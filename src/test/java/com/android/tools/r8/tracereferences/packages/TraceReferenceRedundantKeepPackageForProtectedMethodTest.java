// Copyright (c) 2024, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.tracereferences.packages;

import static org.junit.Assert.assertTrue;

import com.android.tools.r8.TestBase;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.TestParametersCollection;
import com.android.tools.r8.tracereferences.packages.testclasses.TraceReferenceRedundantKeepPackageForProtectedMethodTestClasses;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TraceReferenceRedundantKeepPackageForProtectedMethodTest extends TestBase {

  @Parameter(0)
  public TestParameters parameters;

  @Parameters(name = "{0}")
  public static TestParametersCollection data() {
    return getTestParameters().withNoneRuntime().build();
  }

  @Test
  public void test() throws Exception {
    testForTraceReferences()
        .addInnerClassesAsSourceClasses(getClass())
        .addInnerClassesAsTargetClasses(
            TraceReferenceRedundantKeepPackageForProtectedMethodTestClasses.class)
        .trace()
        .inspect(inspector -> assertTrue(inspector.getPackages().isEmpty()));
  }

  public static class Main
      extends TraceReferenceRedundantKeepPackageForProtectedMethodTestClasses.A {

    public static void main(String[] args) {
      TraceReferenceRedundantKeepPackageForProtectedMethodTestClasses.A.m();
    }
  }
}
