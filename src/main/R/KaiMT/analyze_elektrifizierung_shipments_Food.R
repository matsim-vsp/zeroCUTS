library(ggplot2)
library(dplyr)
library(tidyr)

####
# Erster Aufschlag. Nicht getestet, weil den Plot doch noch gefunden
# Geht um Plots der Veränderung multiple tours (shipments) vs singel tours (Services)
# sowohl gegeneinander, als auch jeweils in dem entsprechenden Setup die Policies vs Base-Case
# KMT Jul 26
###


# Load data
data <- read.csv("berlin_food_scenario.csv")

# Clean up policy names
data$Policy <- gsub("\\(A1\\) ", "", data$Policy)
data$Policy <- gsub("\\(A2\\) ", "", data$Policy)
data$Policy <- gsub("\\(B\\) ", "", data$Policy)
data$Policy <- gsub("\\(C\\) ", "", data$Policy)

# Convert numeric columns
numeric_cols <- c("Costs", "Vehicles", "Distance", "Travel.Time", "Fuel", "CO2")
for (col in numeric_cols) {
  data[[col]] <- as.numeric(data[[col]])
}

# ===== FIGURE 1: Relative changes (multiple vs single tours) =====

# Extract single and multiple tour data
data_s <- data %>% filter(Case == "s") %>% select(-Case)
data_m <- data %>% filter(Case == "m") %>% select(-Case)

# Calculate relative changes (m vs s): (m - s) / s * 100
changes_fig1 <- data.frame(
  Policy = data_s$Policy,
  Costs = ((data_m$Costs - data_s$Costs) / data_s$Costs * 100),
  Vehicles = ((data_m$Vehicles - data_s$Vehicles) / data_s$Vehicles * 100),
  Distance = ((data_m$Distance - data_s$Distance) / data_s$Distance * 100),
  Travel_Time = ((data_m$Travel.Time - data_s$Travel.Time) / data_s$Travel.Time * 100),
  Fuel = ((data_m$Fuel - data_s$Fuel) / data_s$Fuel * 100),
  CO2 = ((data_m$CO2 - data_s$CO2) / data_s$CO2 * 100)
)

# Remove diff rows and reshape to long format
changes_fig1_long <- changes_fig1 %>%
  filter(Policy != "diff") %>%
  pivot_longer(cols = -Policy, names_to = "Metric", values_to = "Change_Percent")

# Reorder metrics and policies for consistent plotting
changes_fig1_long$Metric <- factor(changes_fig1_long$Metric,
  levels = c("Costs", "Vehicles", "Distance", "Travel_Time", "Fuel", "CO2"),
  labels = c("Costs [€]", "# of vehicles", "Distance [km]", "Travel time [h]", 
             "Consumption of diesel [l]", "CO₂ emission [t]"))

changes_fig1_long$Policy <- factor(changes_fig1_long$Policy,
  levels = c("Base", "Cordon toll 20EUR on heavy ICEV",
             "Cordon toll, BEVs available", 
             "No ICEVs in cordon, BEVs available",
             "UCC"))

# Define colors
colors <- c("#1f77b4", "#7f007f", "#6baed6", "#addd8e", "#ff7f0e", "#d62728")

# Create Figure 1
fig1 <- ggplot(changes_fig1_long, aes(x = Policy, y = Change_Percent, fill = Metric)) +
  geom_col(position = "dodge", width = 0.8) +
  scale_fill_manual(values = colors) +
  theme_minimal() +
  theme(
    axis.title.y = element_text(size = 11),
    axis.title.x = element_blank(),
    axis.text.x = element_text(angle = 45, hjust = 1, size = 10),
    axis.text.y = element_text(size = 10),
    legend.position = "bottom",
    legend.title = element_blank(),
    panel.grid.major.y = element_line(color = "gray90", linewidth = 0.3),
    panel.grid.minor = element_blank()
  ) +
  labs(y = "relative changes - multiple to single tours",
       title = "Figure 6.1: Comparison of results for all cases of the Berlin food retailing scenario;\nvehicle can run more than one tour instead of only one single tour.") +
  scale_y_continuous(labels = scales::percent_format(scale = 1), limits = c(-70, 10)) +
  geom_hline(yintercept = 0, color = "black", linewidth = 0.5)

ggsave("figure_6_1_berlin_scenario.png", fig1, width = 14, height = 7, dpi = 300)
print(fig1)

# ===== FIGURE 2: Relative changes compared to base case (multiple tours only) =====

# Extract multiple tours data and exclude diff
data_m_clean <- data_m %>% filter(Policy != "diff")

# Base case values
base_values <- data_m_clean %>% filter(Policy == "Base") %>% select(-Policy)

# Calculate relative changes vs base case for each scenario
changes_fig2 <- data.frame(
  Policy = data_m_clean$Policy[-1],  # Exclude Base
  Costs = ((data_m_clean$Costs[-1] - base_values$Costs[1]) / base_values$Costs[1] * 100),
  Vehicles = ((data_m_clean$Vehicles[-1] - base_values$Vehicles[1]) / base_values$Vehicles[1] * 100),
  Distance = ((data_m_clean$Distance[-1] - base_values$Distance[1]) / base_values$Distance[1] * 100),
  Travel_Time = ((data_m_clean$Travel.Time[-1] - base_values$Travel.Time[1]) / base_values$Travel.Time[1] * 100),
  Fuel = ((data_m_clean$Fuel[-1] - base_values$Fuel[1]) / base_values$Fuel[1] * 100),
  CO2 = ((data_m_clean$CO2[-1] - base_values$CO2[1]) / base_values$CO2[1] * 100)
)

# Reshape to long format
changes_fig2_long <- changes_fig2 %>%
  pivot_longer(cols = -Policy, names_to = "Metric", values_to = "Change_Percent")

# Reorder metrics and policies
changes_fig2_long$Metric <- factor(changes_fig2_long$Metric,
  levels = c("Costs", "Vehicles", "Distance", "Travel_Time", "Fuel", "CO2"),
  labels = c("Costs [€]", "# of vehicles", "Distance [km]", "Travel time [h]", 
             "Consumption of diesel [l]", "CO₂ emission [t]"))

changes_fig2_long$Policy <- factor(changes_fig2_long$Policy,
  levels = c("Cordon toll 20EUR on heavy ICEV",
             "Cordon toll, BEVs available", 
             "No ICEVs in cordon, BEVs available",
             "UCC"),
  labels = c("(A1) Cordon toll 20EUR\nheavy diesel",
             "(A2) Cordon toll, e-\ntrucks available",
             "(B) Prohibition of diesel\ntrucks in cordon",
             "(C) Urban Consolidation\nCenter"))

# Create Figure 2
fig2 <- ggplot(changes_fig2_long, aes(x = Policy, y = Change_Percent, fill = Metric)) +
  geom_col(position = "dodge", width = 0.8) +
  scale_fill_manual(values = colors) +
  theme_minimal() +
  theme(
    axis.title.y = element_text(size = 11),
    axis.title.x = element_blank(),
    axis.text.x = element_text(size = 10),
    axis.text.y = element_text(size = 10),
    legend.position = "bottom",
    legend.title = element_blank(),
    panel.grid.major.y = element_line(color = "gray90", linewidth = 0.3),
    panel.grid.minor = element_blank()
  ) +
  labs(y = "multiple tours: relative changes to base case",
       title = "Figure 6.2: Relative changes for all cases of the Berlin food retailing scenario compared to the\nbase case; vehicle can run more than one tour instead of only one single tour.") +
  scale_y_continuous(labels = scales::percent_format(scale = 1), limits = c(-20, 30)) +
  geom_hline(yintercept = 0, color = "black", linewidth = 0.5)

ggsave("figure_6_2_berlin_scenario.png", fig2, width = 12, height = 7, dpi = 300)
print(fig2)

cat("\n✅ Plots created successfully!\n")
cat("  - figure_6_1_berlin_scenario.png\n")
cat("  - figure_6_2_berlin_scenario.png\n")
