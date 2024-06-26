package com.intellij.driver.sdk.ui

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.ui.components.UiComponent
import com.intellij.driver.sdk.ui.components.button
import com.intellij.driver.sdk.waitFor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// should
infix fun <T : UiComponent> T.should(condition: T.() -> Boolean): T {
  return should(timeout = DEFAULT_FIND_TIMEOUT_SECONDS.seconds, condition = condition)
}

// should not
infix fun <T : UiComponent> T.shouldNot(condition: T.() -> Boolean): T {
  return should(timeout = DEFAULT_FIND_TIMEOUT_SECONDS.seconds, condition = { !condition() })
}

// should
infix fun <T : UiComponent> T.shouldBe(condition: T.() -> Boolean): T {
  return should(timeout = DEFAULT_FIND_TIMEOUT_SECONDS.seconds, condition = condition)
}

fun <T : UiComponent> T.shouldBe(message: String, condition: T.() -> Boolean, timeout: Duration): T {
  return should(message = message, timeout = timeout, condition = condition)
}

fun <T : UiComponent> T.shouldBe(message: String, condition: T.() -> Boolean): T {
  return should(message = message, timeout = DEFAULT_FIND_TIMEOUT_SECONDS.seconds, condition = condition)
}

fun <T : UiComponent> T.shouldBe(condition: T.() -> Boolean, timeout: Duration): T {
  return should(timeout = timeout, condition = condition)
}

// should
infix fun <T : UiComponent> T.shouldHave(condition: T.() -> Boolean): T {
  return should(timeout = DEFAULT_FIND_TIMEOUT_SECONDS.seconds, condition = condition)
}

fun <T : UiComponent> T.shouldHave(message: String, condition: T.() -> Boolean): T {
  return should(message = message, timeout = DEFAULT_FIND_TIMEOUT_SECONDS.seconds, condition = condition)
}

fun <T : UiComponent> T.shouldHave(condition: T.() -> Boolean, timeout: Duration): T {
  return should(timeout = timeout, condition = condition)
}

fun <T : UiComponent> T.shouldHave(message: String, condition: T.() -> Boolean, timeout: Duration): T {
  return should(message = message, timeout = timeout, condition = condition)
}

fun <T : UiComponent> T.should(seconds: Int = DEFAULT_FIND_TIMEOUT_SECONDS, condition: T.() -> Boolean): T {
  return should(timeout = seconds.seconds, condition = condition)
}

fun <T : UiComponent> T.should(message: String,
                               seconds: Int = DEFAULT_FIND_TIMEOUT_SECONDS,
                               condition: T.() -> Boolean): T {
  return should(message = message, timeout = seconds.seconds, condition = condition)
}

fun <T : UiComponent> T.should(message: String = "",
                               timeout: Duration = DEFAULT_FIND_TIMEOUT_SECONDS.seconds,
                               condition: T.() -> Boolean): T {
  var lastException: Throwable? = null
  try {
    waitFor(timeout, errorMessage = message) {
      try {
        this.condition()
      }
      catch (e: Throwable) {
        lastException = e
        false
      }
    }
  } catch (e: WaitForException){
    throw WaitForException(e.duration, e.errorMessage, lastException)
  }
  return this
}

val visible: UiComponent.() -> Boolean = { isVisible() }

val enabled: UiComponent.() -> Boolean = { isEnabled() }

val notEnabled: UiComponent.() -> Boolean = { !isEnabled() }

val present: UiComponent.() -> Boolean = { present() }

val notPresent: UiComponent.() -> Boolean = { notPresent() }

fun haveText(value: String): UiComponent.() -> Boolean = { hasText(value) }

fun haveButton(value: String): UiComponent.() -> Boolean = { button(value).present() }

