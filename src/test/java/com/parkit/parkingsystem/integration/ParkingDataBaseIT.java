package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.management.MemoryType;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.params.ParameterizedTest;

@ExtendWith(MockitoExtension.class)
class ParkingDataBaseIT {
    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    //private Ticket ticket;
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
        when(inputReaderUtil.readSelection()).thenReturn(1); // parses type
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown() {
        dataBasePrepareService.clearDataBaseEntries();
    }
    //Template for Car Parking
    void testParkingACar(String MatReg) throws Exception {
        //ARRANGE
        //ticket assigned
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(MatReg);
        ParkingSpot parkingSpot = new ParkingSpot(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR), ParkingType.CAR, false); //spot is available

        //ACT
        boolean availability = parkingSpotDAO.updateParking(parkingSpot); // we check if the availability is well written in the DB
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        //VERIFY: Checks that a ticket is actually saved in DB and Parking table is updated with availability
        assertNotNull(ticketDAO.getTicket(MatReg)); //ticket is saved in DB
        assertEquals(true, availability); //availability is updated
    }

    @Test
    @DisplayName("A car is correctly parked and its ticket saved in the database")
    void testParkingACar2() throws Exception {
        testParkingACar("ABC");
    }

@ParameterizedTest
@CsvSource({"false, alphabet","true, ABCDEF"}) // case with or without discount ticket once exiting
void testParkingLotExit(boolean discount, String MatReg) throws Exception {
    //ARRANGE
   //if (discount == true) {
   //     parkingSpotDAO = new ParkingSpotDAO();
    //    ticketDAO = new TicketDAO();
    //}
    ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

    testParkingACar(MatReg);
    Thread.sleep(1000);

    Ticket ticket = ticketDAO.getTicket(MatReg);
    Date inTime = new Date();
    Date outTime = new Date();
    ticket.setPrice(0);
    inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
    ticket.setInTime(inTime);
    ticket.setVehicleRegNumber(MatReg);
    ticket.setParkingSpot(ticketDAO.getTicket(MatReg).getParkingSpot());
    if (discount == false) {
        ticketDAO.updateFullTicket(ticket);
        ticket.setOutTime(null);
        ticketDAO.saveTicket(ticket); //refreshes the ticket in the DB with the new inTime
    }
    else {
        ticketDAO.updateFullTicket(ticket); //refreshes the ticket in the DB with the new inTime
    }
    parkingSpotDAO.updateParking(ticket.getParkingSpot());

    //ACT
    parkingService.processExitingVehicle();

    //VERIFY - Check that the fare generated and out time are populated correctly in the database
    assertEquals(true, ticketDAO.updateTicket(ticketDAO.getTicket(MatReg))); //this instruction combines the both transactions needed
}

    @ParameterizedTest
    @CsvSource({"true, ABCDEF"}) // case with or without discount ticket once exiting
    void testParkingLotExit2(boolean discount, String MatReg) throws Exception {
        //ARRANGE
        if (discount == true) {
            parkingSpotDAO = new ParkingSpotDAO();
            ticketDAO = new TicketDAO();
        }
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        testParkingACar(MatReg);
        Thread.sleep(1000);

        //ACT
        parkingService.processExitingVehicle();

        //VERIFY - Check that the fare generated and out time are populated correctly in the database
        assertEquals(true, ticketDAO.updateTicket(ticketDAO.getTicket(MatReg))); //this instruction combines the both transactions needed
    }

    public class Sleeper {
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }
    }
}