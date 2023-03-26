package ru.javawebinar.topjava.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.javawebinar.topjava.model.Meal;
import ru.javawebinar.topjava.util.MealsUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class MealServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(MealServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("forward to meals");
        String action = request.getParameter("action");

        switch (action == null ? "all" : action) {
            case "delete":
//                int id = getId(request);
//                log.info("Delete id={}", id);
//                repository.delete(id);
                response.sendRedirect("meals");
                break;
            case "create":
            case "update":
                final Meal meal = "create".equals(action) ?
                        new Meal(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES), "", 1000) :
                        MealsUtil.meals.get(1);
//                        repository.get(getId(request));
                request.setAttribute("meal", meal);
                request.getRequestDispatcher("/mealForm.jsp").forward(request, response);
                break;
            case "all":
            default:
                log.info("getAll");
//                request.setAttribute("meals",
//                        MealsUtil.getTos(repository.getAll(), MealsUtil.DEFAULT_CALORIES_PER_DAY));
//                request.getRequestDispatcher("/meals.jsp").forward(request, response);

                request.setAttribute("meals", MealsUtil.getTos(MealsUtil.meals, MealsUtil.DEFAULT_CALORIES_PER_DAY));
                request.getRequestDispatcher("/meals.jsp").forward(request, response);
                break;
        }
    }
}
