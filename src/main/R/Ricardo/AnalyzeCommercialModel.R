# Benötigtes Paket laden
library(ggplot2)
library(dplyr)
library(scales)


# Datei einlesen – passe den Pfad ggf. an
data <- read.csv2("C:/Users/erica/shared/matsim-metropole-ruhr/scenarios/metropole-ruhr-v2024.0/output/rvr/commercial_0.1pct/commercialTraffic_Run0pct/analysis/traffic/commercialTraffic_Run0pct.travelDistances_perVehicle.csv", stringsAsFactors = FALSE, fileEncoding = "UTF-8")

# Numerische Spalten umwandeln
data$distanceInKm <- as.numeric(data$distanceInKm)
data$shareOfTravelDistanceWithDepotCharging <- as.numeric(data$shareOfTravelDistanceWithDepotCharging)

# Fahrzeugtypen zusammenfassen
data$vehicleType <- ifelse(data$vehicleType %in% c("truck40t", "heavy40t"),
                           "heavy40t", data$vehicleType)
data$vehicleType <- ifelse(data$vehicleType %in% c("mercedes313", "mercedes313_parcel"),
                           "mercedes313", data$vehicleType)
data$vehicleType <- as.factor(data$vehicleType)

# Berechnung für gesamte Flotte
anteil_alle <- data %>%
  summarise(
    anteil = mean(shareOfTravelDistanceWithDepotCharging <= 1, na.rm = TRUE)
  ) %>%
  mutate(label = paste0(round(anteil * 100, 1), "%"))


# Anteil ≤ 100 % pro Fahrzeugtyp berechnen
anteil_df <- data %>%
  group_by(vehicleType) %>%
  summarise(
    anteil = mean(shareOfTravelDistanceWithDepotCharging <= 1, na.rm = TRUE),
    .groups = "drop"
  ) %>%
  mutate(label = paste0(round(anteil * 100, 1), "%"))

# Anteil ≤ 80 % pro Fahrzeugtyp
anteil80_df <- data %>%
  group_by(vehicleType) %>%
  summarise(
    anteil = mean(shareOfTravelDistanceWithDepotCharging <= 0.8, na.rm = TRUE),
    .groups = "drop"
  ) %>%
  mutate(label = paste0(round(anteil * 100, 1), "%"))

# Anteil ≤ 80 % für gesamte Flotte
anteil80_alle <- data %>%
  summarise(
    anteil = mean(shareOfTravelDistanceWithDepotCharging <= 0.8, na.rm = TRUE)
  ) %>%
  mutate(label = paste0(round(anteil * 100, 1), "%"))


ggplot(data, aes(x = shareOfTravelDistanceWithDepotCharging)) +
  geom_density(fill = "skyblue", alpha = 0.4) +
  geom_rug(sides = "b", alpha = 0.5) +
  # 100%-Strich + Text
  geom_vline(xintercept = 1.0, color = "red", linetype = "dashed", size = 1) +
  geom_text(
    data = anteil_alle,
    aes(x = 1.05, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 2.2,
    color = "red", size = 3.5
  ) +
  # 80%-Strich + Text
  geom_vline(xintercept = 0.8, color = "darkgreen", linetype = "dashed", size = 1) +
  geom_text(
    data = anteil80_alle,
    aes(x = 0.35, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 3.2,
    color = "darkgreen", size = 3.5
  ) +
  scale_x_continuous(
    labels = scales::percent_format(accuracy = 1),
    limits = c(0, NA)
  ) +
  labs(
    x = "Benötigte Reichweite im Verhältnis zur Reichweite des Elektrofahrzeugs ohne Nachladen",
    title = "Dichteverteilung + Rugplot"
  ) +
  theme_minimal()

# Plot 2: Histogramm + Dichteverteilung
ggplot(data, aes(x = shareOfTravelDistanceWithDepotCharging)) +
  geom_histogram(aes(y = ..density..), bins = 75, fill = "lightblue", color = "white", alpha = 0.6) +
  geom_density(color = "darkblue", fill = "blue", alpha = 0.3) +
  geom_vline(xintercept = 1.0, color = "red", linetype = "dashed", size = 1) +
  geom_text(
    data = anteil_alle,
    aes(x = 1.1, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 2.2,
    color = "red", size = 3.5
  ) +
  # 80%-Strich + Text
  geom_vline(xintercept = 0.8, color = "darkgreen", linetype = "dashed", size = 1) +
  geom_text(
    data = anteil80_alle,
    aes(x = 0.35, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 3.2,
    color = "darkgreen", size = 3.5
  ) +
  scale_x_continuous(
    labels = scales::percent_format(accuracy = 1),
    limits = c(0, NA)
  ) +
  labs(
    x = "Anteil der Fahrstrecke mit Depotladung",
    y = "Dichte",
    title = "Histogramm + Dichteverteilung"
  ) +
  theme_minimal()

# Plot 3: Verteilung der Depotladung nach Fahrzeugtyp
ggplot(data, aes(x = shareOfTravelDistanceWithDepotCharging)) +
  geom_histogram(aes(y = ..density..), bins = 30, fill = "lightblue", color = "white", alpha = 0.6) +
  geom_density(color = "darkblue", fill = "blue", alpha = 0.3) +
  geom_vline(xintercept = 1.0, color = "red", linetype = "dashed", size = 1) +
  # Annotation pro vehicleType (nach rechts versetzt)
  geom_text(
    data = anteil_df,
    aes(x = 1.2, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 1.2,
    color = "red", size = 3.5
  ) +
  # 80%-Strich + Text
  geom_vline(xintercept = 0.8, color = "darkgreen", linetype = "dashed", size = 1) +
  geom_text(
    data = anteil80_df,
    aes(x = 1.2, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 3.2,
    color = "darkgreen", size = 3.5
  ) +
  facet_wrap(~ vehicleType, scales = "free_y") +
  scale_x_continuous(
    labels = scales::percent_format(accuracy = 1),
    limits = c(0, NA)
  ) +
  labs(
    x = "Anteil der Fahrstrecke mit Depotladung",
    y = "Dichte",
    title = "Verteilung der Depotladung nach Fahrzeugtyp"
  ) +
  theme_minimal() +
  theme(
    plot.margin = margin(10, 40, 10, 10),
    clip = "off"
  )

# --- PLOT 4: Kumulative Häufigkeit (ECDF) nach vehicleType ---
plot4 <- ggplot(data, aes(x = shareOfTravelDistanceWithDepotCharging)) +
  stat_ecdf(geom = "step", color = "blue", size = 1) +
  facet_wrap(~ vehicleType, scales = "free_y") +
  geom_vline(xintercept = 1.0, color = "red", linetype = "dashed", size = 1) +
  geom_vline(xintercept = 0.8, color = "darkgreen", linetype = "dashed", size = 1) +
  # Text bei 80 %
  geom_text(data = anteil80_df,
            aes(x = 0.82, y = anteil, label = label),
            inherit.aes = FALSE,
            hjust = 0, vjust = -0.5,
            color = "darkgreen", size = 3.5) +
  # Text bei 100 %
  geom_text(data = anteil_df,
            aes(x = 1.02, y = anteil, label = label),
            inherit.aes = FALSE,
            hjust = 0, vjust = -0.5,
            color = "red", size = 3.5) +
  scale_x_continuous(labels = percent_format(accuracy = 1), limits = c(0, 4)) +
  #scale_y_continuous(labels = percent_format(accuracy = 1)) +
  labs(
    x = "Depotladungsanteil",
    y = "Kumulative Verteilung"
  ) +
  theme_minimal()

print(plot4)
