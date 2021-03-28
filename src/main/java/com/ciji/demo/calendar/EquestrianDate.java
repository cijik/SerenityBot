package com.ciji.demo.calendar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class EquestrianDate {

    @Getter
    @Setter
    private int year;

    private EquestrianMonth month;

    @Getter
    @Setter
    private int day;

    public static EquestrianDate fromDays(int days) {
        int equestrianDays = days % 28;
        EquestrianMonth equestrianMonth = EquestrianMonth.values()[days / 28 % 13 - 1];
        int years = days / 28 / 13;
        return new EquestrianDate(years, equestrianMonth, equestrianDays);
    }

    public static EquestrianDate fromDateString(String date) {
        String[] dateValues = date.split("\\.");
        return new EquestrianDate(
                Integer.parseInt(dateValues[2]),
                EquestrianMonth.values()[Integer.parseInt(dateValues[1]) - 1],
                Integer.parseInt(dateValues[0])
        );
    }

    public int getMonth() {
        return month.ordinal() + 1;
    }

    public String getMonthName() {
        return month.toString();
    }

    public void setMonth(int monthNumber) {
        month = EquestrianMonth.values()[monthNumber-1];
    }

    public void setMonth(EquestrianMonth monthName) {
        month = monthName;
    }

    public void addDays(int days) {
        int daysToAdd = days % 28;
        int monthToSet = month.ordinal() + days / 28 % 13;
        int yearsToAdd = days / 28 / 13;

        this.day += daysToAdd;
        if (day > 28) {
            day = daysToAdd % 28;
            monthToSet++;
        }
        if (monthToSet > 13) {
            monthToSet %= 13;
            yearsToAdd++;
        }
        month = EquestrianMonth.values()[monthToSet];
        this.year = yearsToAdd;
    }
}
