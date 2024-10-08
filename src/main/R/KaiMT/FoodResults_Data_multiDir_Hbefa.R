##Fasse ERgebnisse aus den verschiedenen Runs zusammen
## KMT Okt'24

####TODOS
# - Prüfen, dass das Umwandlung in Tabelle für LaTex gut klappt, ggf. Infos anpassen
# - ggf. Rundung der Werte
# -aufs Jahr hochrechnen???



# #setwd("C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax/analysis")
# EFood <- FALSE
setwd("C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_with_rangeConstraint/")
EFood <- FALSE


# Install and load necessary packages
if (!requireNamespace("tidyverse", quietly = TRUE)) {
  install.packages("tidyverse")
}
if (!requireNamespace("plotly", quietly = TRUE)) {
  install.packages("plotly")
}
if (!requireNamespace("gridExtra", quietly = TRUE)) {
  install.packages("gridExtra")
}
library(tidyverse)
library(plotly)
library(gridExtra)
library(tibble)

pollutants2WriteExhaust <- c("Scenario", "CO", "CO2_TOTAL", "NOx", "PM", "PM2_5", "BC_exhaust")
pollutants2WriteNonExhaust <- c("Scenario", "PM_non_exhaust", "PM2_5_non_exhaust", "BC_non_exhaust")


# Hauptverzeichnis, in dem sich die Unterordner befinden
main_dir <- getwd()

# Pfad zum spezifischen Referenzordner
referenz_ordner <- "C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71a_ICEV_NwCE_BVWP_10000it_DCoff_noTax/"

# Liste der Unterordner im Hauptverzeichnis
subdirs <- list.dirs(main_dir, full.names = TRUE, recursive = FALSE)
subdirs <- subdirs[subdirs != referenz_ordner] #Referenzordner soll da nicht drin sein

# Initialisiere einen leeren Dataframe, um die kombinierten Daten zu speichern
kombinierte_daten <- data.frame()

# Lese die Referenzdaten aus dem spezifischen Referenzordner
file_path_referenz <- file.path(referenz_ordner, "Analysis", "1_emissions", "emissionsPerPollutant.csv")


# Überprüfe, ob die Referenzdatei existiert und lese sie ein
if (file.exists(file_path_referenz)) {
  referenzdaten_org <- read.csv(file_path_referenz, sep = ";")
  
  # Füge eine Spalte "ScenarioLang" hinzu und setze den Namen des Referenzordners
  referenzdaten_org$ScenarioLang <- basename(referenz_ordner)
  
  # Speichere die Referenzdaten als erste Zeile im kombinierten DataFrame
  kombinierte_daten <- referenzdaten_org
} else {
  stop(paste("Referenzdatei nicht gefunden in:", file_path_referenz))
}



# Durchlaufe alle Unterordner und lies all die Daten ein.
for (subdir in subdirs) {

  # Erstelle den genauen Pfad zur gewünschten CSV-Datei
  file_path_emissions <- file.path(subdir, "Analysis", "1_emissions", "emissionsPerPollutant.csv")
  
  
  ## Emissions einlesen
  if (file.exists(file_path_emissions)) {
    df_emissions_org <- read.csv(file_path_emissions, sep = ";")
    
    df_emissions <- df_emissions_org # Erstmal nur eine Kopie davon in der dann gearbeitet wird.
    
    # Füge eine Spalte "ScenarioLang" hinzu und weise ihr den Namen des aktuellen Unterordners zu
    df_emissions$ScenarioLang <- basename(subdir)
    
    
    # Füge die Daten zum kombinierten Dataframe hinzu
    kombinierte_daten <- rbind(kombinierte_daten, df_emissions)
    
    
  } else { 
    message(paste("Datei nicht gefunden in:", subdir, file_path_emissions))
    }
  
} ## For Schleife

### Etwas aufräumen:
# Entferne die Spalte "Run"
kombinierte_daten <- kombinierte_daten[, !names(kombinierte_daten) %in% c("Run")]
# Bringe die Spalte "ScenarioLang" an die erste Stelle
kombinierte_daten <- kombinierte_daten[, c("ScenarioLang", setdiff(names(kombinierte_daten), "ScenarioLang"))]

# Erstelle die neue Spalte "Scenario": setze "Base Case", wenn es das Referenzszenario ist, sonst den Teil nach dem letzten Unterstrich
kombinierte_daten$Scenario <- ifelse(
  kombinierte_daten$ScenarioLang == basename(referenz_ordner), 
  "Base Case", 
  sub(".*_", "", kombinierte_daten$ScenarioLang)  # Extrahiert den Teil nach dem letzten Unterstrich
)

# Bringe die Spalte "Scenario" an die zweite Stelle
kombinierte_daten <- kombinierte_daten[, c("ScenarioLang", "Scenario", setdiff(names(kombinierte_daten), c("ScenarioLang", "Scenario")))]


head(kombinierte_daten)


### Relative Änderungen berechnen.
#Funktion um relativeÄnderungen zu berechnen
calcRelChanges <- function(data){
  # Erhalte die numerischen Spalten im kombinierten DataFrame
  numerische_spalten <- sapply(data, is.numeric)

  # Extrahiere die Referenzdaten aus dem kombinierten DataFrame
  referenzdaten <- data[data$ScenarioLang == basename(referenz_ordner), numerische_spalten]

  # Gehe durch alle Szenarien außer dem Referenzszenario und berechne die relative Änderung
  for (i in which(data$ScenarioLang != basename(referenz_ordner))) {
    for (col in names(data)[numerische_spalten]) {
      abs_wert <- as.numeric(data[i, col])  # Sicherstellen, dass es numerisch ist
      ref_wert <- as.numeric(referenzdaten[[col]])        # Sicherstellen, dass es numerisch ist

      # Überprüfen, ob abs_wert und ref_wert numerisch sind
      if (is.na(abs_wert) || is.na(ref_wert)) {
        data[i, col] <- "NA"
      } else if (ref_wert == 0) { # Bei Referenzwert 0 soll das speziell gehandelt werden
        data[i, col] <- paste(abs_wert, " ( -- %)", sep = "")
      } else { # Alles gut, rechne und füge hinzu.
        rel_aenderung <- round(((abs_wert - ref_wert) / ref_wert) * 100, 1)
        data[i, col] <- paste(abs_wert, " (", rel_aenderung, "%)", sep = "")
      }
    }
  }
  data <- data
}




#Funktion zum Schreiben des Outputs
write_output <- function(output_file, dataframe) {
  cat("Erstma nur die ExhaustEmissions\n\n", file = output_file, append = TRUE)
  write.table(dataframe %>% select(all_of(pollutants2WriteExhaust)), file = output_file, sep = ";", row.names = FALSE, col.names = TRUE, append = TRUE)
  cat("\n\nUnd nun noch die Non-exhaustEmissions\n\n", file = output_file, append = TRUE)
  write.table(dataframe %>% select(all_of(pollutants2WriteNonExhaust)), file = output_file, sep = ";", row.names = FALSE, col.names = TRUE, append = TRUE)
  #Und nochmal alle Daten
  write.table(dataframe, file = sub("\\.csv$", "_all.csv", output_file), sep = ";", row.names = FALSE, col.names = TRUE)
}


####Berechne relative Änderungen und schreibe es raus
kombinierte_daten_g <- kombinierte_daten
write_output("Emissions_g.csv", calcRelChanges(kombinierte_daten_g))


###Nun noch Umrechnung von g in kg und Ausgabe dessen
kombinierte_daten_kg <- kombinierte_daten
numerische_spalten <- sapply(kombinierte_daten_kg, is.numeric) # Identifiziere alle numerischen Spalten im Dataframe
kombinierte_daten_kg[numerische_spalten] <- kombinierte_daten_kg[numerische_spalten] / 1000

write_output("Emissions_kg.csv", calcRelChanges(kombinierte_daten_kg))
#head(kombinierte_daten_kg)

