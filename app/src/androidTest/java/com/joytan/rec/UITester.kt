package com.joytan.rec


import androidx.test.espresso.Espresso.onView
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.junit.Test
import org.junit.runner.RunWith

import com.joytan.rec.main.MainActivity
import org.junit.Rule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.runner.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@LargeTest
class UITester {

    @get:Rule
    var activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)


    @Test
    fun test() {
        onView(withId(R.id.fab_record)).perform(click())
        onView(withId(R.id.fab_play)).check(matches(isDisplayed()))
        onView(withId(R.id.fab_delete)).perform(click())
        onView(withId(R.id.fab_record)).perform(click())
        onView(withId(R.id.fab_play)).check(matches(isDisplayed()))
        onView(withId(R.id.fab_delete)).perform(click())
        onView(withId(R.id.fab_setting)).perform(click())
        onView(withText("アプリについて")).perform(click())
        pressBack()
        pressBack()
    }


}