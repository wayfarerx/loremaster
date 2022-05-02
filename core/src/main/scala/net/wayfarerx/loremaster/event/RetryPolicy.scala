/* RetryPolicy.scala
 *
 * Copyright (c) 2022 wayfarerx (@thewayfarerx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.wayfarerx.loremaster
package event

import java.time.Instant

import scala.concurrent.duration.*

import configuration.*

/**
 * A policy for determining when to retry an event.
 *
 * @param backoff     The policy to use when calculating retry backoff.
 * @param termination The policy to use when evaluating if retryPolicy should continue.
 */
case class RetryPolicy(
  backoff: RetryPolicy.Backoff = RetryPolicy.Backoff.Default,
  termination: RetryPolicy.Termination = RetryPolicy.Termination.Default
):

  /**
   * Returns the retry delay to use if the specified event should be retried.
   *
   * @tparam T The type of event to evaluate.
   * @param event The event to evaluate.
   * @return The retry delay to use if the specified event should be retried.
   */
  def apply[T: Event](event: T): Option[FiniteDuration] =
    if termination(event) then None else Some(backoff(event))

  /* Encode the retry policy. */
  override def toString: String = s"$backoff:$termination"

/**
 * Factory for retry policies.
 */
object RetryPolicy extends ((RetryPolicy.Backoff, RetryPolicy.Termination) => RetryPolicy) :

  import Configuration.Data

  /** The separator of retry backoffs and retry terminations. */
  private[this] val Separator = ':'

  /** Support for retry policies as configuration data. */
  given Data[RetryPolicy] = Data.define(classOf[RetryPolicy].getSimpleName) { data =>
    data indexOf Separator match
      case notFound if notFound < 0 =>
        Data[Backoff].apply(data).map(backoff => Default.copy(backoff = backoff))
      case foundAt =>
        Data[Backoff].apply(data.substring(0, foundAt)) ->
          Data[Termination].apply(data drop foundAt dropWhile (_ == Separator)) match
          case (None, None) => None
          case (Some(backoff), None) => Some(Default.copy(backoff = backoff))
          case (None, Some(termination)) => Some(Default.copy(termination = termination))
          case (Some(backoff), Some(termination)) => Some(RetryPolicy(backoff, termination))
  }

  /** The default retry policy. */
  val Default: RetryPolicy = RetryPolicy()

  /**
   * A policy for calculating retry backoff.
   */
  sealed trait Backoff:

    /**
     * Calculates the backoff for retrying the specified event.
     *
     * @tparam T The type of event to examine.
     * @param event The event to examine.
     * @return The backoff for retrying the specified event.
     */
    def apply[T: Event](event: T): FiniteDuration

  /**
   * Factory for backoff policies.
   */
  object Backoff:

    /** Support for retry backoff policies as configuration data. */
    given Data[Backoff] = Data.define(s"${Data[RetryPolicy].`type`}.${classOf[Backoff].getSimpleName}") {
      case golden if golden.headOption contains Golden.Designator =>
        Data[FiniteDuration] apply golden.dropWhile(_ == Golden.Designator) map Golden.apply
      case linear if linear.headOption contains Linear.Designator =>
        Data[FiniteDuration] apply linear.dropWhile(_ == Linear.Designator) map Linear.apply
      case other =>
        Data[FiniteDuration] apply other map Constant.apply
    }

    /** The default backoff policy. */
    val Default: Backoff = Golden(1.minute.toSeconds.seconds)

    /**
     * Returns a constant backoff.
     *
     * @param delay The backoff to always return.
     */
    case class Constant(delay: FiniteDuration) extends Backoff :

      /* Return the constant backoff. */
      override def apply[T: Event](event: T): FiniteDuration = delay

      /* Encode the constant backoff. */
      override def toString: String = delay.toString

    /**
     * Returns a backoff that increases by `delay` after every attempt.
     *
     * @param delay The delay to increase the backoff by after every attempt.
     */
    case class Linear(delay: FiniteDuration) extends Backoff :

      /* Increase the backoff after every attempt. */
      override def apply[T: Event](event: T): FiniteDuration =
        delay * (Event[T].previousAttempts(event) + 1)

      /* Encode the linear backoff. */
      override def toString: String = s"${Linear.Designator}$delay"

    /**
     * Factory for linear backoffs.
     */
    object Linear extends (FiniteDuration => Linear) :

      /** The designator for linear backoffs. */
      val Designator: Char = '+'

    /**
     * Returns a backoff that is multiplied by the golden ratio after every attempt.
     *
     * @param delay The backoff to use for the initial retry.
     */
    case class Golden(delay: FiniteDuration) extends Backoff :

      /* Multiply the backoff by the golden ratio after every attempt. */
      override def apply[T: Event](event: T): FiniteDuration =
        delay * math.pow(8.0 / 5.0, Event[T].previousAttempts(event)).round

      /* Encode the golden backoff. */
      override def toString: String = s"${Golden.Designator}$delay"

    /**
     * Factory for golden backoffs.
     */
    object Golden extends (FiniteDuration => Golden) :

      /** The designator for golden backoffs. */
      val Designator: Char = '~'

  /**
   * A policy that determines when retry operations should terminate.
   */
  sealed trait Termination:

    /**
     * Determines if the specified event should terminate.
     *
     * @tparam T The type of event to examine.
     * @param event The event to examine.
     * @return True if the specified event should terminate.
     */
    def apply[T: Event](event: T): Boolean

  /**
   * Factory for termination policies.
   */
  object Termination:

    /** Support for retry termination policies as configuration data. */
    given Data[Termination] = Data.define("RetryPolicy.Termination") { data =>
      Data[Int] apply data map LimitRetries.apply orElse (Data[FiniteDuration] apply data map LimitDuration.apply)
    }

    /** The default backoff policy. */
    val Default: Termination = LimitRetries(2)

    /**
     * Limits the number of retry attempts.
     *
     * @param maximum The maximum number of retry attempts.
     */
    case class LimitRetries(maximum: Int) extends Termination :

      /* Limit the number of retry attempts. */
      override def apply[T: Event](event: T): Boolean =
        Event[T].previousAttempts(event) >= maximum

      /* Encode the limit on retryPolicy. */
      override def toString: String = maximum.toString

    /**
     * Limits the duration of all retry attempts.
     *
     * @param maximum The maximum duration of all retry attempts.
     */
    case class LimitDuration(maximum: FiniteDuration) extends Termination :

      /* Limit the duration of all retry attempts. */
      override def apply[T: Event](event: T): Boolean =
        Event[T].createdAt(event) plusMillis maximum.toMillis isBefore Instant.now

      /* Encode the limit on duration. */
      override def toString: String = maximum.toString