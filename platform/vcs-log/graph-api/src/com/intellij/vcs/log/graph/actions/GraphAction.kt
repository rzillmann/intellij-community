/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.vcs.log.graph.actions

import com.intellij.vcs.log.graph.PrintElement

/**
 */
interface GraphAction {
  val affectedElement: PrintElement?
  val type: Type

  enum class Type {
    MOUSE_CLICK,
    MOUSE_OVER,
    BUTTON_COLLAPSE,
    BUTTON_EXPAND
  }

  data class GraphActionImpl(override val affectedElement: PrintElement?, override val type: Type) : GraphAction
}
