package com.enes.movyswipe

import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.enes.movyswipe.fragments.EslemeYokFragment
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Küçük ekranlarda / büyük yazı tipinde alttaki butonlara kaydırarak ulaşılabildiğini doğrular.
 * (Eşleşme yok ekranı eskiden kaydırılamıyordu; "Ana Menü" butonu ekran dışında kalıyordu.)
 */
@RunWith(AndroidJUnit4::class)
class SmallScreenLayoutTest {

    @Test
    fun eslesmeYok_alttakiButonlaraKaydirilarakUlasilir() {
        launchFragmentInContainer<EslemeYokFragment>(
            fragmentArgs = bundleOf("checkedCount" to 20),
            themeResId = R.style.Theme_FilmSwipe
        )
        onView(withId(R.id.btnAnaMenu)).perform(scrollTo()).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.btnHizliDuello)).perform(scrollTo()).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.btnTekrarDene)).perform(scrollTo()).check(matches(isCompletelyDisplayed()))
    }

    @Test
    fun eslesmeYok_gercekFilmSayisiniGosterir() {
        launchFragmentInContainer<EslemeYokFragment>(
            fragmentArgs = bundleOf("checkedCount" to 20),
            themeResId = R.style.Theme_FilmSwipe
        )
        onView(withId(R.id.tvFilmSayisi)).perform(scrollTo()).check(matches(withText("20 film kontrol edildi")))
    }
}
