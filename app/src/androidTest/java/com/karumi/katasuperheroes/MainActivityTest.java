/*
 * Copyright (C) 2015 Karumi.
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

package com.karumi.katasuperheroes;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.karumi.katasuperheroes.di.MainComponent;
import com.karumi.katasuperheroes.di.MainModule;
import com.karumi.katasuperheroes.model.SuperHero;
import com.karumi.katasuperheroes.model.SuperHeroesRepository;
import com.karumi.katasuperheroes.recyclerview.RecyclerViewInteraction;
import com.karumi.katasuperheroes.ui.view.MainActivity;
import com.karumi.katasuperheroes.ui.view.SuperHeroDetailActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.cosenonjaviste.daggermock.DaggerMockRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.karumi.katasuperheroes.matchers.RecyclerViewItemsCountMatcher.recyclerViewHasItemCount;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class) @LargeTest public class MainActivityTest {

  @Rule public DaggerMockRule<MainComponent> daggerRule =
      new DaggerMockRule<>(MainComponent.class, new MainModule()).set(
          new DaggerMockRule.ComponentSetter<MainComponent>() {
            @Override public void setComponent(MainComponent component) {
              SuperHeroesApplication app =
                  (SuperHeroesApplication) InstrumentationRegistry.getInstrumentation()
                      .getTargetContext()
                      .getApplicationContext();
              app.setComponent(component);
            }
          });

  @Rule public IntentsTestRule<MainActivity> activityRule =
      new IntentsTestRule<>(MainActivity.class, true, false);

  @Mock SuperHeroesRepository repository;

  @Test public void showsEmptyCaseIfThereAreNoSuperHeroes() {
    givenThereAreNoSuperHeroes();

    startActivity();

    onView(withText("¯\\_(ツ)_/¯")).check(matches(isDisplayed()));
  }

  @Test public void doesNotShowEmptyCaseIfThereAreNoSuperHeroes() {
    //config mock to return any number of super heroes
    givenThereAreSomeSuperHeroes(10, false);

    //open the view
    startActivity();

    //check the empty case is not shown
    onView(withText("¯\\_(ツ)_/¯")).check(matches(not(isDisplayed())));
  }

  @Test public void showsTheNumberOfSuperHeroes() {
    givenThereAreSomeSuperHeroes(10, false);

    startActivity();

    onView(withId(R.id.recycler_view)).check(matches(recyclerViewHasItemCount(10)));
  }

  @Test public void showsSuperHeroesName() {
    int numberOfSuperHeroes = 1000;
    givenThereAreSomeSuperHeroes(numberOfSuperHeroes, false);

    startActivity();

    for (int i = 0; i < numberOfSuperHeroes; i++) {
      onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.scrollToPosition(i));
      onView(withText("SuperHero - " + i)).check(matches(isDisplayed()));
    }
  }

  @Test public void showsAvengersBadgeIfASuperHeroIsPartOfTheAvengersTeam() {
    int numberOfSuperHeroes = 10;
    List<SuperHero> superHeroes = givenThereAreSomeSuperHeroes(numberOfSuperHeroes, true);

    startActivity();

    RecyclerViewInteraction.<SuperHero>onRecyclerView(withId(R.id.recycler_view))
            .withItems(superHeroes)
            .check(new RecyclerViewInteraction.ItemViewAssertion<SuperHero>() {
              @Override public void check(SuperHero superHero, View view, NoMatchingViewException e) {
                matches(hasDescendant(allOf(withId(R.id.iv_avengers_badge),
                        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))).check(view, e);
              }
            });
  }

  @Test public void doesNotShowAvengersBadgeIfASuperHeroIsNotPartOfTheAvengersTeam() {
    int numberOfSuperHeroes = 10;
    List<SuperHero> superHeroes = givenThereAreSomeSuperHeroes(numberOfSuperHeroes, false);

    startActivity();

    RecyclerViewInteraction.<SuperHero>onRecyclerView(withId(R.id.recycler_view))
            .withItems(superHeroes)
            .check(new RecyclerViewInteraction.ItemViewAssertion<SuperHero>() {
              @Override public void check(SuperHero superHero, View view, NoMatchingViewException e) {
                matches(hasDescendant(allOf(withId(R.id.iv_avengers_badge),
                        withEffectiveVisibility(ViewMatchers.Visibility.GONE)))).check(view, e);
              }
            });
  }

  @Test public void showSuperHeroDetailActivityOnTap() {
      int numberOfSuperHeroes = 10;
      List<SuperHero> superHeroes = givenThereAreSomeSuperHeroes(numberOfSuperHeroes, false);

      startActivity();

      int superHeroIndex = 0;

      onView(withId(R.id.recycler_view)).
              perform(RecyclerViewActions.actionOnItemAtPosition(superHeroIndex, click()));

      SuperHero selectedSuperHero = superHeroes.get(superHeroIndex);
      //Two different ways to check if the navigation has been done
      intended(hasComponent(SuperHeroDetailActivity.class.getCanonicalName()));
      intended(hasExtra("super_hero_name_key", selectedSuperHero.getName()));
  }

  private void givenThereAreNoSuperHeroes() {
    when(repository.getAll()).thenReturn(Collections.<SuperHero>emptyList());
  }

  private List<SuperHero> givenThereAreSomeSuperHeroes(int numberOfSuperHeroes, boolean avengers) {
    List<SuperHero> superHeroes = new ArrayList<>();
    for (int i = 0; i < numberOfSuperHeroes; i++) {
      String heroName = "SuperHero - " + i;
      String photo = "https://i.annihil.us/u/prod/marvel/i/mg/9/b0/537bc2375dfb9.jpg";
      String description = "Hero description " + i;
      SuperHero superHero = new SuperHero(heroName, photo, avengers, description);
      superHeroes.add(superHero);
      when(repository.getByName(heroName)).thenReturn(superHero);
    }
    when(repository.getAll()).thenReturn(superHeroes);
    return superHeroes;
  }

  private MainActivity startActivity() {
    return activityRule.launchActivity(null);
  }
}