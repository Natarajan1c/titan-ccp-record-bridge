package titan.ccp.kiekerbridge.raritan.simulator;

import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import titan.ccp.common.configuration.Configurations;

/**
 * Runs a simulation by setting up simulated sensors, reading data from them and pushing them to a
 * destination.
 */
public class SimulationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimulationRunner.class);

  private final HttpPusher httpPusher;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

  private final AtomicLong counter = new AtomicLong(0);

  private final Collection<SensorReader> sensorReaders;

  public SimulationRunner(final URI uri, final Collection<SimulatedSensor> sensors) {
    this(uri, sensors, false);
  }

  /**
   * Create a new simulation runner.
   */
  public SimulationRunner(final URI uri, final Collection<SimulatedSensor> sensors,
      final boolean sendTimestampsInMs) {
    this.httpPusher = new HttpPusher(uri);
    this.sensorReaders = sensors.stream().map(s -> new SensorReader(s, sendTimestampsInMs))
        .collect(Collectors.toList());
  }

  /**
   * Starts the simulation.
   */
  public void run() {
    for (final SensorReader sensorReader : this.sensorReaders) {
      final Runnable sender = () -> {
        this.httpPusher.sendMessage(sensorReader.getMessage());
        this.counter.addAndGet(1);
      };

      this.scheduler.scheduleAtFixedRate(sender, 0, sensorReader.getSensor().getPeroid().toMillis(),
          TimeUnit.MILLISECONDS);
    }

  }

  public final long getCounter() {
    return this.counter.get();
  }

  public void shutdown() {
    this.scheduler.shutdownNow();
  }

  /**
   * Main method to start a simulation runner using external configurations.
   */
  public static void main(final String[] args) throws InterruptedException {
    // Turn off Java's DNS caching
    java.security.Security.setProperty("networkaddress.cache.ttl", "0"); // TODO

    final Configuration configuration = Configurations.create();
    final String setupType = configuration.getString("setup", "demo"); // NOCS
    if (setupType != "demo") {
      throw new IllegalArgumentException(
          "Different execution modes (setups) are no longer supported.");
    }

    LOGGER.info("Start Simulator");
    final URI pushUri = URI.create(configuration.getString("kieker.bridge.address"));
    new SimulationRunner(pushUri, getDemoSetup()).run();

  }

  /**
   * Get simulated sensors for a demo setup.
   */
  public static List<SimulatedSensor> getDemoSetup() {
    return List.of(
        new SimulatedSensor(
            "server1",
            Duration.ofSeconds(1), // NOCS
            FunctionBuilder.of(x -> 50).plus(Functions.wave1()).plus(Functions.noise(10)).build()), // NOCS
        new SimulatedSensor(
            "server2",
            Duration.ofSeconds(2), // NOCS
            FunctionBuilder.of(x -> 60).plus(Functions.noise(20)).build()), // NOCS
        new SimulatedSensor(
            "server3",
            Duration.ofSeconds(1), // NOCS
            FunctionBuilder.of(x -> 30) // NOCS
                .plusScaled(20, Functions.squares(4 * 60_000, 100_000, 5 * 60_000)) // NOCS
                .plus(Functions.noise(5)).build()), // NOCS
        new SimulatedSensor(
            "printer1",
            Duration.ofSeconds(1), // NOCS
            FunctionBuilder.of(x -> 10) // NOCS
                .plusScaled(80, Functions.squares(5 * 60_000, 15 * 60_000, 35 * 60_000)) // NOCS
                .plus(Functions.noise(20)).build()), // NOCS
        new SimulatedSensor(
            "printer2",
            Duration.ofSeconds(2), // NOCS
            FunctionBuilder.of(x -> 5) // NOCS
                .plusScaled(60, Functions.squares(1 * 60_000, 12 * 60_000, 19 * 60_000)) // NOCS
                .plus(Functions.noise(10)).build()), // NOCS
        new SimulatedSensor(
            "fan1",
            Duration.ofSeconds(3), // NOCS
            FunctionBuilder.of(x -> 30).plus(Functions.wave2()).plus(Functions.noise(5)).build()), // NOCS
        new SimulatedSensor(
            "ac1",
            Duration.ofSeconds(1), // NOCS
            FunctionBuilder.of(Functions.wave3()).plus(Functions.noise(5)).build()), // NOCS
        new SimulatedSensor(
            "ac2",
            Duration.ofSeconds(1), // NOCS
            FunctionBuilder.of(Functions.wave3()).plus(Functions.noise(5)).build())); // NOCS
  }

}
