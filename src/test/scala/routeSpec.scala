/**
 * Copyright (C) 2011 Osinka <http://osinka.ru>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osinka.camel.scala

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.apache.camel.{Exchange,Processor,Predicate,RuntimeTransformException}
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.scala.{ScalaProcessor, ScalaPredicate}

@RunWith(classOf[JUnitRunner])
class RouteSpec extends FunSpec with CamelSpec with MustMatchers with Implicits {
  describe("Processor/DSL") {
    it("should process in") {
      def f(x: Int): Int = x+1
      val e = processExchange(in(classOf[Int]) {f _}) { _.in = 1 }
      e.in[Int] must equal(2)
    }
    it("should process in -> in") {
      val e = processExchange(in(classOf[Int]) {1+} .toIn) { _.in = 1 }
      e.in[Int] must equal(2)
    }
    it("should process in -> out") {
      val e = processExchange(in(classOf[Int]) {1+} .toOut) { _.in = 1 }
      e.out must equal(2)
    }
    it("should process out ->") {
      val e = processExchange(out(classOf[Int]) {1+}) { _.out = 1 }
      e.in must equal(2)
    }
    it("should process out -> in") {
      val e = processExchange(out(classOf[Int]) {1+} .toIn) { _.out = 1 }
      e.in must equal(2)
    }
    it("should process out -> out") {
      val e = processExchange(out(classOf[Int]) {1+} .toOut) { _.out = 1 }
      e.out must equal(2)
    }
    it("should not modify exchange when function returns Unit") {
      def fn(i: Int) { }
      val e = processExchange(in(classOf[Int]) {fn _}) { _.in = 1}
      e.in must equal(1)
    }
    it("should raise exception when trying to set In when function returns Unit") {
      def fn(i: Int) { }
      evaluating { processExchange(in(classOf[Int]) {fn _} .toIn) { _.in = 1} } must produce [RuntimeTransformException]
    }
    it("should raise exception when trying to set Out when function returns Unit") {
      def fn(i: Int) { }
      evaluating { processExchange(in(classOf[Int]) {fn _} .toOut) { _.in = 1} } must produce [RuntimeTransformException]
    }
  }
  describe("Predicate/DSL") {
    it("should filter in") {
      filterExchange(in(classOf[Int]) {1==}) { _.in = 1 } must equal(true)
    }
    it("should raise exception when trying to filter when function returns Unit") {
      def fn(i: Int) { }
      evaluating { filterExchange(in(classOf[Int]) {fn _}) { _.in = 1} } must produce [RuntimeTransformException]
    }
  }
  describe("PartialFunction/DSL") {
    sealed trait AlgoType
    case object LeafOne extends AlgoType
    case object LeafTwo extends AlgoType

    it("should leave message body if it's not in function domain") {
      val p: Processor = in(classOf[AlgoType]) collect {
        case LeafOne => LeafTwo
      }

      val e = processExchange(p) { _.in = LeafTwo }
      e.in[AlgoType] must equal(LeafTwo)
    }
    it("should process body if it's in function domain") {
      val p: Processor = in(classOf[AlgoType]) collect {
        case LeafOne => LeafTwo
      }

      val e = processExchange(p) { _.in = LeafOne }
      e.in[AlgoType] must equal(LeafTwo)
    }
    it("should filter") {
      val p: Predicate = in(classOf[AlgoType]) collect {
        case LeafOne => true
      }

      filterExchange(p) { _.in = LeafOne } must equal(true)
      filterExchange(p) { _.in = LeafTwo } must equal(false)
    }
  }

  def processExchange(p: Processor)(pre: Exchange => Unit) = {
    val e = createExchange
    pre(e)
    p.process(e)
    e
  }

  def filterExchange(f: Predicate)(pre: Exchange => Unit) = {
    val e = createExchange
    pre(e)
    f.matches(e)
  }
}
