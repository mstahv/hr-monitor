import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.vaadin.Application;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication
public class DevApplication {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
