package org.noway.kottage

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test

class TestResourceOperationsTest(manager: TestResourceManager) : KottageTest(manager) {

    @Test
    fun testExecutingOperationWithUnitReturnValueShouldSucceed() {
        val operations = TestResourceOperations(manager)
        val returnVal = operations.executeReversibleOperation({}, {},
                                                              "testExecutingOperationWithUnitReturnValueShouldSucceed")

        assertThat(returnVal, Matchers.instanceOf(Unit.javaClass))
    }

    @Test
    fun testExecutingOperationWithAnyReturnValueShouldSucceed() {
        val operations = TestResourceOperations(manager)
        val returnVal = operations.executeReversibleOperation({ "test" }, { test -> print(test) },
                                                              "testExecutingOperationWithAnyReturnValueShouldSucceed")

        assertThat(returnVal, Matchers.instanceOf(String::class.java))
        assertThat(returnVal, `is`("test"))
    }

    @Test
    fun testReverseOperationsReturnValueShouldBeIgnored() {
        //there is no use for a return value from reverse operation, so ignore it
        val operations = TestResourceOperations(manager)
        operations.executeReversibleOperation({ "test" }, { test -> "" + test },
                                              "testReverseOperationsReturnValueShouldBeIgnored")
        operations.reverseAfterTest()
    }

    @Test
    fun testNonUnitResultShouldBeSuppliedToReverseOperation() {
        var reverseParam: String? = null
        val operations = TestResourceOperations(manager)
        operations.executeReversibleOperation({ "test" }, { reverseParam = it },
                                              "testNonUnitResultShouldBeSuppliedToReverseOperation")
        operations.reverseAfterTest()

        assertThat(reverseParam, notNullValue())
        assertThat(reverseParam, `is`("test"))
    }


    @Test
    fun testUnitResultShouldBeSuppliedToReverseOperation() {
        var reverseParamTypeIsUnit = false
        val operations = TestResourceOperations(manager)
        operations.executeReversibleOperation({}, { reverseParamTypeIsUnit = it is Unit },
                                              "testUnitResultShouldBeSuppliedToReverseOperation")
        operations.reverseAfterTest()

        assertThat("Expected a parameter of type Unit for reverse operation", reverseParamTypeIsUnit, `is`(true))
    }


    @Test
    fun testRegisteringOperationWithNullReturnValueShouldFail() {
        // null expression does not compile
        // operations.executeReversibleOperation({null}, {}, "", "")
    }

    @Test
    fun testRegisteringOperationWithNullableReturnValueShouldFail() {
        // nullable expression does not compile
        //operations.executeReversibleOperation({methodReturningANullableType()}, {}, "", "")
    }

    fun methodReturningANullableType(): String? {
        return if (System.currentTimeMillis() % 2 == 0L) null else "odd"
    }


}