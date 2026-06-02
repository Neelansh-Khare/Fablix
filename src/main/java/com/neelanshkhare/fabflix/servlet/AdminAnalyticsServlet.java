package com.neelanshkhare.fabflix.servlet;

import com.neelanshkhare.fabflix.util.RedisUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/admin/analytics")
public class AdminAnalyticsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        JSONObject result = new JSONObject();

        try {
            // Get cache stats
            String posterHitsStr = RedisUtil.get("stats:cache:poster:hits");
            String posterMissesStr = RedisUtil.get("stats:cache:poster:misses");
            String autoHitsStr = RedisUtil.get("stats:cache:autocomplete:hits");
            String autoMissesStr = RedisUtil.get("stats:cache:autocomplete:misses");

            result.put("posterHits", posterHitsStr != null ? Long.parseLong(posterHitsStr) : 0);
            result.put("posterMisses", posterMissesStr != null ? Long.parseLong(posterMissesStr) : 0);
            result.put("autocompleteHits", autoHitsStr != null ? Long.parseLong(autoHitsStr) : 0);
            result.put("autocompleteMisses", autoMissesStr != null ? Long.parseLong(autoMissesStr) : 0);

            // Get popular searches with scores
            JSONArray popularSearchesArray = new JSONArray();
            if (RedisUtil.isRedisAvailable()) {
                if (RedisUtil.getCluster() != null) {
                    List<Tuple> topSearches = RedisUtil.getCluster().zrevrangeWithScores(RedisUtil.POPULAR_SEARCHES_KEY, 0, 9);
                    for (Tuple tuple : topSearches) {
                        JSONObject searchObj = new JSONObject();
                        searchObj.put("query", tuple.getElement());
                        searchObj.put("score", tuple.getScore());
                        popularSearchesArray.put(searchObj);
                    }
                }
            }
            result.put("popularSearches", popularSearchesArray);

            out.print(result.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("message", "Error retrieving analytics: " + e.getMessage());
            out.print(error.toString());
        }
    }
}
