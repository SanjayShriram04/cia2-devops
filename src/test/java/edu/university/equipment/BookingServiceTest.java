package edu.university.equipment;
import static org.junit.jupiter.api.Assertions.*; import java.util.NoSuchElementException; import org.junit.jupiter.api.BeforeEach; import org.junit.jupiter.api.Test;
class BookingServiceTest {
 private BookingService service; @BeforeEach void setup(){service=new BookingService();}
 @Test void successfulBookingReducesInventory(){service.create("STU1001","LAPTOP01",2);assertEquals(8,service.getEquipment("LAPTOP01").getAvailableQuantity());}
 @Test void zeroQuantityRejected(){assertThrows(IllegalArgumentException.class,()->service.create("STU1001","LAPTOP01",0));assertEquals(10,service.getEquipment("LAPTOP01").getAvailableQuantity());}
 @Test void blankBorrowerRejected(){assertThrows(IllegalArgumentException.class,()->service.create(" ","LAPTOP01",1));}
 @Test void unknownEquipmentRejected(){assertThrows(NoSuchElementException.class,()->service.create("STU1001","UNKNOWN",1));}
 @Test void ordinaryOverbookingRejected(){assertThrows(IllegalStateException.class,()->service.create("STU1001","PROJECTOR01",6));assertEquals(4,service.getEquipment("PROJECTOR01").getAvailableQuantity());}
 @Test void cancellationRestoresInventoryOnce(){Booking b=service.create("STU1001","TABLET01",2);service.cancel(b.getId());assertEquals(8,service.getEquipment("TABLET01").getAvailableQuantity());assertThrows(IllegalStateException.class,()->service.cancel(b.getId()));}
 @Test void bookingExceedingStockByOneRejected(){Booking b1=service.create("STU1001","PROJECTOR01",2);Booking b2=service.create("STU1002","PROJECTOR01",1);assertEquals(1,service.getEquipment("PROJECTOR01").getAvailableQuantity());assertThrows(IllegalStateException.class,()->service.create("STU1003","PROJECTOR01",2));assertEquals(1,service.getEquipment("PROJECTOR01").getAvailableQuantity());assertEquals(2,service.getBooking(b1.getId()).getQuantity());assertEquals(1,service.getBooking(b2.getId()).getQuantity());}
 @Test void bookingExactlyMatchingStockAccepted(){service.create("STU1001","PROJECTOR01",2);Booking b=service.create("STU1002","PROJECTOR01",2);assertNotNull(b.getId());assertEquals(2,b.getQuantity());assertEquals("CONFIRMED",b.getStatus());assertEquals(0,service.getEquipment("PROJECTOR01").getAvailableQuantity());}
 @Test void bookingTwoUnitsAccepted(){Booking b=service.create("STU1001","LAPTOP01",2);assertEquals(2,b.getQuantity());assertEquals(8,service.getEquipment("LAPTOP01").getAvailableQuantity());}
 @Test void bookingMoreThanTwoUnitsRejected(){assertThrows(IllegalArgumentException.class,()->service.create("STU1001","LAPTOP01",3));assertEquals(10,service.getEquipment("LAPTOP01").getAvailableQuantity());}
}