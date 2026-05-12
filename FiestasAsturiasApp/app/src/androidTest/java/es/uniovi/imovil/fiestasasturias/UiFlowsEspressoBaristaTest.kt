package es.uniovi.imovil.fiestasasturias

import android.Manifest
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import es.uniovi.imovil.fiestasasturias.ui.MainActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matcher
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class UiFlowsEspressoBaristaTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun navegacion_appAbreYBottomNavigationFunciona() {
        onView(withId(R.id.topBar)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))

        clickBottomNavItem(R.id.nav_list)
        onView(withId(R.id.searchInput)).check(matches(isDisplayed()))

        clickBottomNavItem(R.id.nav_settings)
        onView(withId(R.id.switchDarkMode)).check(matches(isDisplayed()))

        clickBottomNavItem(R.id.nav_home)
        assertDisplayed(R.string.title_explore)
    }

    @Test
    fun busqueda_escribeTextoYFiltraResultados() {
        clickBottomNavItem(R.id.nav_list)

        waitForRecyclerItems(activityRule.scenario, R.id.recyclerView)
        val firstTitle = getRecyclerTitleAt(activityRule.scenario, R.id.recyclerView, 0)
        val query = firstTitle.split(" ").first().trim()

        writeTo(R.id.searchInput, query)

        assertDisplayed(query)
    }

    @Test
    fun favoritos_clickEstrellaYApareceEnFavoritos() {
        clickBottomNavItem(R.id.nav_list)
        waitForRecyclerItems(activityRule.scenario, R.id.recyclerView)

        val firstTitle = getRecyclerTitleAt(activityRule.scenario, R.id.recyclerView, 0)

        onView(withId(R.id.recyclerView)).perform(clickChildAtPosition(0, R.id.favIcon))
        clickBottomNavItem(R.id.nav_fav)

        assertDisplayed(firstTitle)
        onView(withId(R.id.searchLayout)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    private fun waitForRecyclerItems(
        scenario: ActivityScenario<MainActivity>,
        recyclerId: Int,
        timeoutMs: Long = 7000L
    ) {
        val start = System.currentTimeMillis()
        var hasItems = false
        while (!hasItems && System.currentTimeMillis() - start < timeoutMs) {
            scenario.onActivity { activity ->
                val recycler = activity.findViewById<RecyclerView>(recyclerId)
                hasItems = (recycler.adapter?.itemCount ?: 0) > 0
            }
            if (!hasItems) Thread.sleep(200)
        }
        assertTrue("No se cargaron items en el RecyclerView", hasItems)
    }

    private fun getRecyclerTitleAt(
        scenario: ActivityScenario<MainActivity>,
        recyclerId: Int,
        position: Int
    ): String {
        var title = ""
        scenario.onActivity { activity ->
            val recycler = activity.findViewById<RecyclerView>(recyclerId)
            recycler.scrollToPosition(position)
            val holder = recycler.findViewHolderForAdapterPosition(position)
            val titleView = holder?.itemView?.findViewById<android.widget.TextView>(R.id.title)
            title = titleView?.text?.toString()?.trim().orEmpty()
        }
        assertTrue("El titulo de la fiesta esta vacio", title.isNotBlank())
        return title
    }

    private fun clickChildAtPosition(position: Int, childId: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = withId(R.id.recyclerView)

            override fun getDescription(): String = "Click on child view at position"

            override fun perform(uiController: UiController, view: View) {
                val recyclerView = view as RecyclerView
                recyclerView.scrollToPosition(position)
                uiController.loopMainThreadUntilIdle()
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                val child = viewHolder?.itemView?.findViewById<View>(childId)
                child?.performClick()
                uiController.loopMainThreadUntilIdle()
            }
        }
    }

    private fun clickBottomNavItem(menuItemId: Int) {
        onView(
            allOf(
                withId(menuItemId),
                isDescendantOfA(withId(R.id.bottomNav)),
                isDisplayed()
            )
        ).perform(click())
    }
}
