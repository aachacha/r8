// Copyright (c) 2021, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.optimize.argumentpropagation.codescanner;

import com.android.tools.r8.graph.AppView;
import com.android.tools.r8.graph.DexType;
import com.android.tools.r8.ir.analysis.type.DynamicType;
import com.android.tools.r8.ir.analysis.value.AbstractValue;
import com.android.tools.r8.optimize.argumentpropagation.utils.WideningUtils;
import com.android.tools.r8.shaking.AppInfoWithLiveness;
import com.android.tools.r8.utils.Action;

public class BottomClassTypeValueState extends BottomValueState {

  private static final BottomClassTypeValueState INSTANCE = new BottomClassTypeValueState();

  private BottomClassTypeValueState() {}

  public static BottomClassTypeValueState get() {
    return INSTANCE;
  }

  @Override
  public ValueState mutableJoin(
      AppView<AppInfoWithLiveness> appView,
      ValueState state,
      DexType staticType,
      StateCloner cloner,
      Action onChangedAction) {
    if (state.isBottom()) {
      return this;
    }
    if (state.isUnknown()) {
      return state;
    }
    assert state.isConcrete();
    assert state.asConcrete().isReferenceState();
    ConcreteReferenceTypeValueState concreteState = state.asConcrete().asReferenceState();
    AbstractValue abstractValue = concreteState.getAbstractValue(appView);
    DynamicType dynamicType = concreteState.getDynamicType();
    DynamicType widenedDynamicType =
        WideningUtils.widenDynamicNonReceiverType(appView, dynamicType, staticType);
    if (concreteState.isClassState() && !widenedDynamicType.isUnknown()) {
      return cloner.mutableCopy(concreteState);
    }
    return abstractValue.isUnknown() && widenedDynamicType.isUnknown()
        ? unknown()
        : new ConcreteClassTypeValueState(
            abstractValue, widenedDynamicType, concreteState.copyInFlow());
  }
}
