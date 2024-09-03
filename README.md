# The first ever Vaadin based heart rate monitor application

This is a trivial WebBluetooth integration example for Vaadin. 
It can connect to heart rate belts that advertise themselves in a specific manner.
Requires a [compatible browser](https://caniuse.com/web-bluetooth) (Chrome on a non-iOS device will do fine).

TIP: Best experience with Polar H10, that provides also raw ECG data.

Check the code for the details, that is the documentation :-)

## Running the application
The project is a standard Maven project. To run it from the command line,
type `mvn`, then open http://localhost:8080 in your browser.

You can also import the project to your IDE of choice as you would with any
Maven project. Read more on [how to set up a development environment for
Vaadin projects](https://vaadin.com/docs/latest/guide/install) (Windows, Linux, macOS).

If you are not interested about the web tech, but just your heart rate, you can also use a deployed version:

https://hr.dokku1.parttio.org/

## Deploying to Production
To create a production build, call `mvn clean package -Pproduction`.
This will build a JAR file with all the dependencies and front-end resources,
ready to be deployed. The file can be found in the `target` folder after the build completes.

Once the JAR file is built, you can run it using
`java -jar target/myapp-1.0-SNAPSHOT.jar` (NOTE, replace 
`myapp-1.0-SNAPSHOT.jar` with the name of your jar).

## Project structure

- `MainView.java` in `src/main/java` is an example Vaadin view.
- `src/main/resources` contains configuration files and static resources
- The `frontend` directory in the root folder is where client-side 
  dependencies and resource files should be placed.

## Useful links

- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Follow the tutorials at [vaadin.com/tutorials](https://vaadin.com/tutorials).
- Watch training videos and get certified at [vaadin.com/learn/training]( https://vaadin.com/learn/training).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Search UI components and their usage examples at [vaadin.com/components](https://vaadin.com/components).
- Find a collection of solutions to common use cases in [Vaadin Cookbook](https://cookbook.vaadin.com/).
- Find Add-ons at [vaadin.com/directory](https://vaadin.com/directory).
- Ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/vaadin) or join our [Discord channel](https://discord.gg/MYFq5RTbBn).
- Report issues, create pull requests in [GitHub](https://github.com/vaadin/).
