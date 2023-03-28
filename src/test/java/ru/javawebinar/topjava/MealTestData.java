package ru.javawebinar.topjava;

import ru.javawebinar.topjava.model.Meal;
import ru.javawebinar.topjava.model.Role;
import ru.javawebinar.topjava.model.User;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.javawebinar.topjava.model.AbstractBaseEntity.START_SEQ;

public class MealTestData {
    public static final int MEAL_USER_ID = START_SEQ + 3;
    public static final int MEAL_ADMIN_ID = START_SEQ + 9;

    public static final Meal meal = new Meal(MEAL_USER_ID, LocalDateTime.of(2020, Month.JANUARY, 30, 10, 0), "Завтрак", 500);
    public static final Meal meal1 = new Meal(MEAL_USER_ID + 1, LocalDateTime.of(2020, Month.JANUARY, 30, 13, 0), "Обед", 1000);
    public static final Meal meal2 = new Meal(MEAL_USER_ID + 2, LocalDateTime.of(2020, Month.JANUARY, 30, 20, 0), "Ужин", 500);
    public static final Meal meal3 = new Meal(MEAL_USER_ID + 3, LocalDateTime.of(2020, Month.JANUARY, 31, 11, 0), "Завтрак", 500);
    public static final Meal meal4 = new Meal(MEAL_USER_ID + 4, LocalDateTime.of(2020, Month.JANUARY, 31, 14, 0), "Обед", 1000);
    public static final Meal meal5 = new Meal(MEAL_USER_ID + 5, LocalDateTime.of(2020, Month.JANUARY, 31, 21, 0), "Ужин", 500);
    public static final Meal meal_admin = new Meal(MEAL_ADMIN_ID, LocalDateTime.of(2020, Month.JUNE, 1, 14, 0), "Админ ланч", 510);
    public static final Meal meal_admin1 = new Meal(MEAL_ADMIN_ID + 1, LocalDateTime.of(2020, Month.JUNE, 1, 21, 0), "Админ ланч", 1500);

    public static final List<Meal> meals = Arrays.asList(meal, meal1, meal2, meal3, meal4, meal5);

    public static Meal getNew() {
        return new Meal(null, LocalDateTime.of(2020, Month.JANUARY, 29, 21, 0), "Test meal", 1500);
    }

    public static Meal getUpdated() {
        return new Meal(MEAL_USER_ID, meal.getDateTime(), "Update meal", 333);
    }

    public static void assertMatch(Meal actual, Meal expected) {
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    public static void assertMatch(Iterable<Meal> actual, Meal... expected) {
        assertMatch(actual, Arrays.asList(expected));
    }

    public static void assertMatch(Iterable<Meal> actual, Iterable<Meal> expected) {
        assertThat(actual).isEqualTo(expected);
    }
}
