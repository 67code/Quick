package Quick.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import Quick.dto.BookServiceDto;
import Quick.service.ServiceBooking.BookService;


@RestController
@RequestMapping("api/bookservice")
public class BookServiceController {

    private BookService _bookService;

    public BookServiceController(BookService bookService) {
        _bookService = bookService;
    }
@PostMapping("/RequestBooking")
    public ResponseEntity<?> bookService(@RequestBody BookServiceDto bookServiceDto) {
        try {
           
            _bookService.createBookingRequest(bookServiceDto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error Booking service: " + e.getMessage());
        }
    }
@GetMapping("/GetBookingRequests/{userId}")
    public ResponseEntity<?> getBookingRequests(@PathVariable long userId) {
        try {
            return ResponseEntity.ok(_bookService.getBookingRequests(userId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching booking requests: " + e.getMessage());
        }
    }

    
}
