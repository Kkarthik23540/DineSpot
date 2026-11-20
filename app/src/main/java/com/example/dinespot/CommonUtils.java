package com.example.dinespot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CommonUtils {

    /**
     * Checks if the restaurant should be open based on current time and its operating hours.
     * 
     * @param restaurant The restaurant object containing opening and closing times.
     * @return True if current time is within operating hours, false otherwise.
     */
    public static boolean isWithinOperatingHours(CommonRestaurant restaurant) {
        String openStr = restaurant.getOpeningTime();
        String closeStr = restaurant.getClosingTime();

        if (openStr == null || openStr.isEmpty() || closeStr == null || closeStr.isEmpty()) {
            return true; // Assume open if no hours specified
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date now = sdf.parse(sdf.format(new Date()));
            Date openTime = sdf.parse(openStr);
            Date closeTime = sdf.parse(closeStr);

            if (openTime == null || closeTime == null || now == null) return true;

            // Handle cases where closing time is after midnight (e.g. 11 PM to 02 AM)
            if (closeTime.before(openTime)) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(closeTime);
                cal.add(Calendar.DAY_OF_MONTH, 1);
                closeTime = cal.getTime();
                
                if (now.before(openTime)) {
                    cal.setTime(now);
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    now = cal.getTime();
                }
            }

            return now.after(openTime) && now.before(closeTime);

        } catch (ParseException e) {
            e.printStackTrace();
            return true;
        }
    }
}
