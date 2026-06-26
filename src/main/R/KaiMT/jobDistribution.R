# Job Distribution - Häufigkeitsverteilung der Auftragsgrößen
# Liest 2echelon_JobSplitting.csv und plottet Verteilungen je Spalte

library(ggplot2)
library(tidyr)
library(dplyr)

# Dateipfade
input_file <- file.path("input", "kmt", "2echelon_JobSplitting.csv")
output_file <- file.path("output", "kmt", "job_distribution_plot.png")

# Daten lesen (Semikolon-Trenner, Meta-Zeilen überspringen)
data <- read.csv(
  input_file,
  sep = ";",
  header = FALSE,
  skip = 3,
  stringsAsFactors = FALSE
)

# Spaltennamen setzen (basierend auf Struktur: Kaufland original, 18t, 8t; Edeka original, 18t, 8t)
colnames(data) <- c("Kaufland_original", "Kaufland_18t", "Kaufland_8t",
                    "Edeka_original", "Edeka_18t", "Edeka_8t")

# Daten in long-Format umwandeln für ggplot
data_long <- data %>%
  pivot_longer(
    cols = everything(),
    names_to = "Kategorie",
    values_to = "Auftragsgroesse"
  ) %>%
  filter(!is.na(Auftragsgroesse) & Auftragsgroesse != "")

# Auftragsgroesse in numerisch umwandeln
data_long$Auftragsgroesse <- as.numeric(data_long$Auftragsgroesse)

# Plot erstellen
p <- ggplot(data_long, aes(x = Auftragsgroesse)) +
  geom_histogram(
    aes(y = after_stat(count)),
    bins = 15,
    fill = "steelblue",
    color = "black",
    alpha = 0.7
  ) +
  facet_wrap(~ Kategorie, scales = "free_y") +
  labs(
    title = "Häufigkeitsverteilung der Auftragsgrößen",
    subtitle = "2-Echelon Job Splitting - Kaufland und Edeka",
    x = "Auftragsgröße",
    y = "Häufigkeit"
  ) +
  theme_minimal() +
  theme(
    strip.background = element_rect(fill = "lightgray"),
    strip.text = element_text(size = 10, face = "bold"),
    axis.text = element_text(size = 9)
  )

# Output-Verzeichnis erstellen falls nicht vorhanden
output_dir <- dirname(output_file)
if (!dir.exists(output_dir)) {
  dir.create(output_dir, recursive = TRUE)
}

# Plot speichern
ggsave(output_file, plot = p, width = 12, height = 8, dpi = 150)

cat("Plot gespeichert unter:", output_file, "\n")

# Auch als Konsolen-Ausgabe der Verteilungen
cat("\nVerteilungsstatistik:\n")
print(data_long %>%
        group_by(Kategorie) %>%
        summarise(
          n = sum(!is.na(Auftragsgroesse)),
          min = min(Auftragsgroesse, na.rm = TRUE),
          max = max(Auftragsgroesse, na.rm = TRUE),
          mean = mean(Auftragsgroesse, na.rm = TRUE),
          median = median(Auftragsgroesse, na.rm = TRUE)
        ))
