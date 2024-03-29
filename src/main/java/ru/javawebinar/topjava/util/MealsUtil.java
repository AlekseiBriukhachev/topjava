package ru.javawebinar.topjava.util;

import ru.javawebinar.topjava.model.Meal;
import ru.javawebinar.topjava.to.MealTo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

public class MealsUtil {

    private MealsUtil() {
    }

    public static List<MealTo> getTos(Collection<Meal> meals, int caloriesPerDay) {
        return filterByPredicate(meals, caloriesPerDay, meal -> true);
    }

    public static List<MealTo> getFilteredTos(Collection<Meal> meals, int caloriesPerDay, LocalTime startTime, LocalTime endTime) {
        return filterByPredicate(meals, caloriesPerDay, meal -> Util.isBetweenHalfOpen(meal.getTime(), startTime, endTime));
    }

    private static List<MealTo> filterByPredicate(Collection<Meal> meals, int caloriesPerDay, Predicate<Meal> filter) {
        Map<LocalDate, Integer> caloriesSumByDate = meals.stream()
                .collect(
                        Collectors.groupingBy(Meal::getDate, Collectors.summingInt(Meal::getCalories))
                );

        return meals.stream()
                .filter(filter)
                .map(meal -> createTo(meal, caloriesSumByDate.get(meal.getDate()) > caloriesPerDay))
                .toList();
    }

    public static List<MealTo> filteredByCycles(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {

        final Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        meals.forEach(meal -> caloriesSumByDate.merge(meal.getDate(), meal.getCalories(), Integer::sum));

        final List<MealTo> mealsTo = new ArrayList<>();
        meals.forEach(meal -> {
            if (Util.isBetweenHalfOpen(meal.getTime(), startTime, endTime)) {
                mealsTo.add(createTo(meal, caloriesSumByDate.get(meal.getDate()) > caloriesPerDay));
            }
        });
        return mealsTo;
    }

    private static List<MealTo> filteredByRecursion(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        ArrayList<MealTo> result = new ArrayList<>();
        filterWithRecursion(new LinkedList<>(meals), startTime, endTime, caloriesPerDay, new HashMap<>(), result);
        return result;
    }

    private static void filterWithRecursion(LinkedList<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay,
                                            Map<LocalDate, Integer> dailyCaloriesMap, List<MealTo> result) {
        if (meals.isEmpty()) return;

        Meal meal = meals.pop();
        dailyCaloriesMap.merge(meal.getDate(), meal.getCalories(), Integer::sum);
        filterWithRecursion(meals, startTime, endTime, caloriesPerDay, dailyCaloriesMap, result);
        if (Util.isBetweenHalfOpen(meal.getTime(), startTime, endTime)) {
            result.add(createTo(meal, dailyCaloriesMap.get(meal.getDate()) > caloriesPerDay));
        }
    }

    private static List<MealTo> filteredBySetterRecursion(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        class MealNode {
            private final MealNode prev;
            private final MealTo mealTo;

            public MealNode(MealTo mealTo, MealNode prev) {
                this.mealTo = mealTo;
                this.prev = prev;
            }

            public void setExcess() {
                mealTo.setExcess(true);
                if (prev != null) {
                    prev.setExcess();
                }
            }
        }

        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        Map<LocalDate, MealNode> mealNodeByDate = new HashMap<>();
        List<MealTo> mealsTo = new ArrayList<>();
        meals.forEach(meal -> {
            LocalDate localDate = meal.getDate();
            boolean excess = caloriesSumByDate.merge(localDate, meal.getCalories(), Integer::sum) > caloriesPerDay;
            if (Util.isBetweenHalfOpen(meal.getTime(), startTime, endTime)) {
                MealTo mealTo = createTo(meal, excess);
                mealsTo.add(mealTo);
                if (!excess) {
                    MealNode prevNode = mealNodeByDate.get(localDate);
                    mealNodeByDate.put(localDate, new MealNode(mealTo, prevNode));
                }
            }
            if (excess) {
                MealNode mealNode = mealNodeByDate.remove(localDate);
                if (mealNode != null) {
                    // recursive set for all interval day meals
                    mealNode.setExcess();
                }
            }
        });
        return mealsTo;
    }

    public static List<MealTo> filteredByRecursionWithCycleAndRunnable(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        List<MealTo> mealsTo = new ArrayList<>();
        Iterator<Meal> iterator = meals.iterator();

        new Runnable() {
            @Override
            public void run() {
                while (iterator.hasNext()) {
                    Meal meal = iterator.next();
                    caloriesSumByDate.merge(meal.getDate(), meal.getCalories(), Integer::sum);
                    if (Util.isBetweenHalfOpen(meal.getTime(), startTime, endTime)) {
                        run();
                        mealsTo.add(createTo(meal, caloriesSumByDate.get(meal.getDate()) > caloriesPerDay));
                    }
                }
            }
        }.run();
        return mealsTo;
    }

    private static List<MealTo> filteredByExecutor(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) throws InterruptedException {
        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        List<Callable<Void>> tasks = new ArrayList<>();
        final List<MealTo> mealsTo = Collections.synchronizedList(new ArrayList<>());

        meals.forEach(meal -> {
            caloriesSumByDate.merge(meal.getDate(), meal.getCalories(), Integer::sum);
            if (Util.isBetweenHalfOpen(meal.getTime(), startTime, endTime)) {
                tasks.add(() -> {
                    mealsTo.add(createTo(meal, caloriesSumByDate.get(meal.getDate()) > caloriesPerDay));
                    return null;
                });
            }
        });
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.invokeAll(tasks);
        executorService.shutdown();
        return mealsTo;
    }

    public static List<MealTo> filteredByLock(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) throws InterruptedException {
        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        List<MealTo> mealsTo = new ArrayList<>();
        ExecutorService executor = Executors.newCachedThreadPool();
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        for (Meal meal : meals) {
            caloriesSumByDate.merge(meal.getDateTime().toLocalDate(), meal.getCalories(), Integer::sum);
            if (Util.isBetweenHalfOpen(meal.getDateTime().toLocalTime(), startTime, endTime))
                executor.submit(() -> {
                    lock.lock();
                    mealsTo.add(createTo(meal, caloriesSumByDate.get(meal.getDateTime().toLocalDate()) > caloriesPerDay));
                    lock.unlock();
                });
        }
        lock.unlock();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        return mealsTo;
    }

    private static List<MealTo> filteredByCountDownLatch(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) throws InterruptedException {
        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        List<MealTo> mealsTo = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latchCycles = new CountDownLatch(meals.size());
        CountDownLatch latchTasks = new CountDownLatch(meals.size());
        for (Meal meal : meals) {
            caloriesSumByDate.merge(meal.getDateTime().toLocalDate(), meal.getCalories(), Integer::sum);
            if (Util.isBetweenHalfOpen(meal.getDateTime().toLocalTime(), startTime, endTime)) {
                new Thread(() -> {
                    try {
                        latchCycles.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mealsTo.add(createTo(meal, caloriesSumByDate.get(meal.getDateTime().toLocalDate()) > caloriesPerDay));
                    latchTasks.countDown();
                }).start();
            } else {
                latchTasks.countDown();
            }
            latchCycles.countDown();
        }
        latchTasks.await();
        return mealsTo;
    }

    public static List<MealTo> filteredByPredicate(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        List<MealTo> mealsTo = new ArrayList<>();

        Predicate<Boolean> predicate = b -> true;
        for (Meal meal : meals) {
            caloriesSumByDate.merge(meal.getDateTime().toLocalDate(), meal.getCalories(), Integer::sum);
            if (Util.isBetweenHalfOpen(meal.getDateTime().toLocalTime(), startTime, endTime)) {
                predicate = predicate.and(b -> mealsTo.add(createTo(meal, caloriesSumByDate.get(meal.getDateTime().toLocalDate()) > caloriesPerDay)));
            }
        }
        predicate.test(true);
        return mealsTo;
    }

    public static List<MealTo> filteredByConsumerChain(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        Map<LocalDate, Integer> caloriesPerDays = new HashMap<>();
        List<MealTo> result = new ArrayList<>();
        Consumer<Void> consumer = dummy -> {
        };

        for (Meal meal : meals) {
            caloriesPerDays.merge(meal.getDate(), meal.getCalories(), Integer::sum);
            if (Util.isBetweenHalfOpen(meal.getTime(), startTime, endTime)) {
                consumer = consumer.andThen(dummy -> result.add(createTo(meal, caloriesPerDays.get(meal.getDateTime().toLocalDate()) > caloriesPerDay)));
            }
        }
        consumer.accept(null);
        return result;
    }

    private static List<MealTo> filteredByFlatMap(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        Collection<List<Meal>> list = meals.stream()
                .collect(Collectors.groupingBy(Meal::getDate)).values();

        return list.stream()
                .flatMap(dayMeals -> {
                    boolean excess = dayMeals.stream().mapToInt(Meal::getCalories).sum() > caloriesPerDay;
                    return dayMeals.stream().filter(meal ->
                                    Util.isBetweenHalfOpen(meal.getTime(), startTime, endTime))
                            .map(meal -> createTo(meal, excess));
                }).collect(toList());
    }

    private static List<MealTo> filteredByCollector(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        final class Aggregate {
            private final List<Meal> dailyMeals = new ArrayList<>();
            private int dailySumOfCalories;

            private void accumulate(Meal meal) {
                dailySumOfCalories += meal.getCalories();
                if (Util.isBetweenHalfOpen(meal.getTime(), startTime, endTime)) {
                    dailyMeals.add(meal);
                }
            }

            // never invoked if the upstream is sequential
            private Aggregate combine(Aggregate that) {
                this.dailySumOfCalories += that.dailySumOfCalories;
                this.dailyMeals.addAll(that.dailyMeals);
                return this;
            }

            private Stream<MealTo> finisher() {
                final boolean excess = dailySumOfCalories > caloriesPerDay;
                return dailyMeals.stream().map(meal -> createTo(meal, excess));
            }
        }

        Collection<Stream<MealTo>> values = meals.stream()
                .collect(Collectors.groupingBy(Meal::getDate,
                        Collector.of(Aggregate::new, Aggregate::accumulate, Aggregate::combine, Aggregate::finisher))
                ).values();

        return values.stream().flatMap(identity()).collect(toList());
    }

    public static MealTo createTo(Meal meal, boolean excess) {
        return new MealTo(meal.getId(), meal.getDateTime(), meal.getDescription(), meal.getCalories(), excess);
    }
}
