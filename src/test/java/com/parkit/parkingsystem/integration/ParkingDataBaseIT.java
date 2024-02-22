package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(MockitoExtension.class)
class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private Ticket ticket = new Ticket();
    private FareCalculatorService fareCalculatorService = new FareCalculatorService();
    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(2); // parses type
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown() {

    }

    @Test
    @DisplayName("A car is correctly parked and its ticket saved in the database")
    void testParkingACar() {
        //ARRANGE
        //ticket assigned
        Ticket ticket = new Ticket();
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        ticket.setInTime(inTime);

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false); //spot is available
        parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        ticket.setVehicleRegNumber("ABCDE");
        ticket.setParkingSpot(parkingSpot);
        boolean availability = parkingSpotDAO.updateParking(parkingSpot); // we check if the availability is well written in the DB

        //ACT
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        //VERIFY: Checks that a ticket is actually saved in DB and Parking table is updated with availability
        assertEquals(true, ticketDAO.saveTicket(ticket)); //ticket is saved in DB
        assertEquals(true, availability); //availability is updated
    }


    @ParameterizedTest
    @ValueSource(booleans = {false, true}) // case with or without discount ticket once exiting
    void testParkingLotExit(boolean discount) {
        testParkingACar();
        //ARRANGE
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        ticket.setInTime(inTime);
        Date outTime = new Date();
        ticket.setOutTime(outTime);

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false); //spot is available
        parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        ticket.setVehicleRegNumber("ABCDE");
        ticket.setParkingSpot(parkingSpot);
        parkingSpotDAO.updateParking(parkingSpot);

        FareCalculatorService fareCalculatorService = new FareCalculatorService();
        fareCalculatorService.calculateFare(ticket, discount); // the user does not have a discount ticket

        //ACT
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        //VERIFY - Check that the fare generated and out time are populated correctly in the database
        assertEquals(true, ticketDAO.updateTicket(ticket)); //this instruction combines the both transactions needed
    }
}
