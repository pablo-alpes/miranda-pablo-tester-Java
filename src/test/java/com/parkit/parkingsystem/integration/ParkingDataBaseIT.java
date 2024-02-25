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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
        when(inputReaderUtil.readSelection()).thenReturn(1); // parses type
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown() {

    }

    @Test
    @DisplayName("A car is correctly parked and its ticket saved in the database")
    void testParkingACar(String MatReg) throws Exception {
        //ARRANGE
        //ticket assigned
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(MatReg);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false); //spot is available

        boolean availability = parkingSpotDAO.updateParking(parkingSpot); // we check if the availability is well written in the DB

        //ACT
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        //VERIFY: Checks that a ticket is actually saved in DB and Parking table is updated with availability
        assertNotNull(ticketDAO.getTicket(MatReg)); //ticket is saved in DB
        assertEquals(true, availability); //availability is updated
    }

    @Test
    @DisplayName("A car is correctly parked and its ticket saved in the database")
    void testParkingACar2() throws Exception {
        //ARRANGE
        //ticket assigned
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false); //spot is available

        boolean availability = parkingSpotDAO.updateParking(parkingSpot); // we check if the availability is well written in the DB

        //ACT
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        //VERIFY: Checks that a ticket is actually saved in DB and Parking table is updated with availability
        assertNotNull(ticketDAO.getTicket("ABCDEF")); //ticket is saved in DB
        assertEquals(true, availability); //availability is updated
    }

@ParameterizedTest
@CsvSource({"true, ABCDEF", "false, MICKEY"}) // case with or without discount ticket once exiting
void testParkingLotExit(boolean discount, String MatReg) throws Exception {
    //ARRANGE
    if (discount == true) {
        parkingSpotDAO = new ParkingSpotDAO();
        ticketDAO = new TicketDAO();
    }
    testParkingACar(MatReg);
    Thread.sleep(1000);


    //ACT
    ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    parkingService.processExitingVehicle();

    //VERIFY - Check that the fare generated and out time are populated correctly in the database
    assertEquals(true, ticketDAO.updateTicket(ticketDAO.getTicket(MatReg))); //this instruction combines the both transactions needed
}
}