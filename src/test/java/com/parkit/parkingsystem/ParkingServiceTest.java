package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static junit.framework.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    private static ParkingService parkingService;

    //Dependencies in ParkingService
    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @Nested
    class exit {
        @BeforeEach
        public void setUpPerTest() {
            try {
                when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

                ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
                Ticket ticket = new Ticket();
                ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
                ticket.setParkingSpot(parkingSpot);
                ticket.setVehicleRegNumber("ABCDEF");
                when(ticketDAO.getTicket(anyString())).thenReturn(ticket);

                parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to set up test mock objects");
            }
        }


        @Test
        @DisplayName("Checks exit is OK")
        void processExitingVehicleTest() {
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
            when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(true);
            parkingService.processExitingVehicle();
            verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        }

        @Test
        @DisplayName("Issue once exiting - No ticket update")
        void processExitingVehicleTestUnableUpdate() {
            when(ticketDAO.updateTicket(ticket)).thenReturn(false);
            parkingService.processExitingVehicle();
            verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
        }
    }

    @Test
    @DisplayName("Best case - The access of a Car goes OK")
    void testProcessIncomingVehicle() {
        try { //because of read selection, we need to capture the exception
            //ARRANGE - mock behaviour expected for the dependencies of the method .ProcessIncomingVehicle

            //Context : InteractiveShell
            //First step - getNextParkingNumberIfAvailable
            when(inputReaderUtil.readSelection()).thenReturn(1); // Choice : 1 incoming vehicle
            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1); //Returns 1 if a slot is available
            //Second step: getVehicleRegNumber
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("MICKEYMOUSE"); //indicates car number

            // ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true); //allocates successfully a parking spot
            when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true); // defines the ticket is successfully saved in the database

            //ACT
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO); //Instantiates the object withe corresponding mocks for this scenario
            parkingService.processIncomingVehicle(); //launches now the method under test

            //ASSERT
            verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class)); //ensures ticket is saved in the DB, so all previous actions are correctly executed
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    @DisplayName("Issue No available spot")
    void test_GetNextParkingNumberAvailable_NoAvailableSpot() {
        //error :
        //2024-02-17 19:46:15 [main] ERROR ParkingService:80 - Error fetching next available parking slot
        //java.lang.Exception: Error fetching parking number from DB. Parking slots might be full
        try { //because of read selection, we need to capture the exception
            //ARRANGE - mock behaviour expected for the dependencies of the method .ProcessIncomingVehicle

            //First step - Car entry and parking spot is null
            when(inputReaderUtil.readSelection()).thenReturn(1); // Choice : 1 incoming vehicle
            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0); //Defines unexpected behaviour : 0

            //ACT
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO); //Instantiates the object withe corresponding mocks for this scenario, null values for parkijngspot and ticketdao
            //launches now the method under test

            //ASSERT
            assertNull(parkingService.getNextParkingNumberIfAvailable());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    @DisplayName("Issue - Wrong Vehicle Argument passed")
    void test_GetNextParkingNumberAvailable_WrongArgument() {
        try { //because of read selection, we need to capture the exception
            //ARRANGE
            //Vehicle wrong argument
            when(inputReaderUtil.readSelection()).thenReturn(3); // Choice : 3 - Wrong argument case

            //ACT
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO); //Instantiates the object withe corresponding mocks for this scenario, null values for parkijngspot and ticketdao

            //ASSERT
            assertNull(parkingService.getNextParkingNumberIfAvailable());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    @DisplayName("Available spot ID 1")
    void test_GetNextParkingNumberAvailable_ParkingFound() {
        try { //because of read selection, we need to capture the exception
            //ARRANGE - mock behaviour expected for the dependencies of the method .ProcessIncomingVehicle

            //First step - Car entry and parking spot is null
            when(inputReaderUtil.readSelection()).thenReturn(1); // Choice : 1 incoming vehicle
            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1); //Defines the parkingNumber id = 1

            //ACT
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO); //Instantiates the object withe corresponding mocks for this scenario, null values for parkijngspot and ticketdao
            //launches now the method under test

            //ASSERT
            assertNotNull(parkingService.getNextParkingNumberIfAvailable());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }
}