/**
 * Service for Price Calculation
 *
 * It receives A ticket and stamps the entry and exit of the Car or Bike accordingly. Based
 * on it, it calculates the prices according the parking time and the fixed rates.
 * Integrates the fixed on the time conversion.
 *
 * @version: 1.0
 * @author: Pablo Miranda
 * @date: 15-01-2024
 *
 */

package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }
        //FIXED : Hour converted to ms
        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        //FIXED - Conversion to fraction of hours (int was insufficient to detect portions of hour)
        long duration = (outHour - inHour);
        float hours = (float) (duration) /1000/60/60; // Cast of duration to allow decimals

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(hours * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                ticket.setPrice(hours * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default: throw new IllegalArgumentException("Unknown Parking Type");
        }
    }
}