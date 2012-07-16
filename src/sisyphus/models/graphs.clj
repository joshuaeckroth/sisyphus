(ns sisyphus.models.graphs
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.results :only [csv-filenames results-to-csv]])
  (:use sisyphus.models.common))

(defn type-filename
  [doc graph ftype]
  (format "%s/%s-%s-%s.%s" cachedir
          (:_id doc) (:_id graph) (:_rev graph) ftype))

(defn list-graphs
  []
  (let [all-graphs (:rows (view "graphs-list"))
        problems (set (map (comp first :key) all-graphs))]
    (reduce (fn [m problem]
              (assoc m problem
                     (map :value (filter (fn [g] (= problem (first (:key g))))
                                         all-graphs))))
            {} problems)))

(defn get-graph
  [problem n]
  (:value (first (:rows (view "graphs-list" {:key [problem n]})))))

;; graphs for simulations are set in the run
(defn set-graphs
  [runid graphs run-or-sim]
  (let [doc (get-doc runid)]
    (reset-doc-cache runid)
    (clutch/with-db db
      (clutch/update-document doc {(if (= "run" run-or-sim) :graphs
                                       :simulation-graphs) graphs}))))

(defn new-graph
  [graph]
  (create-doc (assoc graph :type "graph")))

(defn update-graph
  [graph]
  (let [doc (get-doc (:id graph))]
    (reset-doc-cache (:id graph))
    (clutch/with-db db
      (clutch/update-document doc (dissoc graph :id :_id :_rev)))))

(defn update-graph-attachment
  [doc fname graph ftype theme width height]
  (reset-doc-cache (:_id doc))
  (try
    (clutch/with-db db
      (clutch/update-attachment doc fname
                                (format "%s-%s-%s-%s-%s-%s" (:_id graph) (:_rev graph) ftype
                                   theme width height)
                                (cond (= "png" ftype) "image/png"
                                      (= "pdf" ftype) "application/pdf"
                                      :else "application/octet-stream")))
    (catch Exception e)))

(def theme_minimal
  "my_palette <- c(\"#3465a4\", \"#2e3436\", \"#f57900\")
   theme_minimal <- function (base_size = 12, base_family = \"\") {
   structure(list(
       axis.line = theme_blank(),
       axis.text.x = theme_text(family = base_family, size = base_size * 0.8, lineheight = 0.9, vjust = 1), 
       axis.text.y = theme_text(family = base_family, size = base_size * 0.8, lineheight = 0.9, hjust = 1),
       axis.ticks = theme_segment(colour = \"black\", size = 0.2),
       axis.title.x = theme_text(family = base_family, size = base_size, vjust = 0),
       axis.title.y = theme_text(family = base_family, size = base_size, angle = 90, vjust = 0.5),
       axis.ticks.length = unit(0.3, \"lines\"),
       axis.ticks.margin = unit(0.5, \"lines\"),
       legend.background = theme_rect(colour = NA), 
       legend.margin = unit(0.2, \"cm\"),
       legend.key = theme_rect(colour = NA), 
       legend.key.size = unit(1.2, \"lines\"),
       legend.key.height = NULL, 
       legend.key.width = NULL,
       legend.text = theme_text(family = base_family, size = base_size * 0.8),
       legend.text.align = NULL, 
       legend.title = theme_text(family = base_family, size = base_size * 0.8, face = \"bold\", hjust = 0),
       legend.title.align = NULL, 
       legend.position = \"right\",
       legend.direction = \"vertical\",
       legend.justification = \"center\",
       legend.box = NULL,
       panel.background = theme_rect(fill = \"white\", colour = NA),
       panel.border = theme_rect(fill = NA, colour = \"grey90\"),
       panel.grid.major = theme_line(colour = \"grey90\", size = 0.2),
       panel.grid.minor = theme_line(colour = \"grey98\", size = 0.5),
       panel.margin = unit(0.25, \"lines\"), 
       strip.background = theme_rect(fill = NA, colour = NA), 
       strip.text.x = theme_text(family = base_family, size = base_size * 0.8),
       strip.text.y = theme_text(family = base_family, size = base_size * 0.8, angle = -90),
       plot.background = theme_rect(colour = NA), 
       plot.title = theme_text(family = base_family, size = base_size * 1.2),
       plot.margin = unit(c(1, 1, 0.5, 0.5), \"lines\")),
     class = \"options\")
   }")

(def theme_poster
  "tango_text <- theme_text(colour = \"#2e3436\")
   my_palette <- c(\"#3465a4\", \"#2e3436\", \"#f57900\")
   theme_poster <- function (base_size = 12, base_family = \"\") {
   structure(list(
       axis.line = theme_blank(),
       axis.text.x = tango_text,
       axis.text.y = tango_text,
       axis.ticks = theme_segment(colour = \"black\", size = 0.2),
       axis.title.x = tango_text,
       axis.title.y = theme_text(angle = 90),
       axis.ticks.length = unit(0.3, \"lines\"),
       axis.ticks.margin = unit(0.5, \"lines\"),
       legend.background = theme_rect(colour = NA), 
       legend.margin = unit(0.2, \"cm\"),
       legend.key = theme_rect(colour = NA), 
       legend.key.size = unit(1.2, \"lines\"),
       legend.key.height = NULL, 
       legend.key.width = NULL,
       legend.text = tango_text,
       legend.text.align = NULL, 
       legend.title = tango_text,
       legend.title.align = NULL, 
       legend.position = \"bottom\",
       legend.direction = \"vertical\",
       legend.justification = \"center\",
       legend.box = NULL,
       panel.background = theme_rect(fill = \"white\", colour = NA),
       panel.border = theme_rect(fill = NA, colour = \"#2e3436\", size = 1),
       panel.grid.major = theme_line(colour = \"#d3d7cf\", size = 0.2),
       panel.grid.minor = theme_line(colour = \"#eeeeec\", size = 0.5),
       panel.margin = unit(0.25, \"lines\"), 
       strip.background = theme_rect(fill = NA, colour = NA), 
       strip.text.x = tango_text,
       strip.text.y = tango_text,
       plot.background = theme_rect(colour = NA), 
       plot.title = theme_text(family = base_family, size = base_size * 1.2),
       plot.margin = unit(c(1, 1, 0.5, 0.5), \"lines\")),
     class = \"options\")
   }")

(defn render-graph-file
  [doc graph ftype theme width height]
  (reset-doc-cache (:_id doc))
  (if (get-attachment (:_id doc) (format "%s-%s-%s-%s-%s-%s" (:_id graph) (:_rev graph) ftype
                                    theme width height))
    {:success true}
    (let [csv-fnames (csv-filenames doc)
          ftype-fname (type-filename doc graph ftype)
          tmp-fname (format "%s/%s-%s-%s.rscript"
                       cachedir (:_id doc) (:_id graph) (:_rev graph))
          rcode (format "library(ggplot2)\nlibrary(grid)\n%s\n%s\n%s\n
                         p <- p + theme_%s()\n
                         p <- p + scale_colour_manual(values=my_palette)\n
                         ggsave(\"%s\", plot = p, dpi = %d, width = %s, height = %s)"
                   (format "%s\n%s\n" theme_minimal theme_poster)
                   (apply str (map #(format "%s <- read.csv(\"%s\")\n"
                                     (name %) (get csv-fnames %))
                                 (keys csv-fnames)))
                   (:code graph) theme ftype-fname
                   (if (= "pdf" ftype) 300 100) width height)]
      (results-to-csv doc csv-fnames)
      ;; save rcode to file
      (with-open [writer (io/writer tmp-fname)]
        (.write writer rcode))
      ;; run Rscript
      (let [status (sh "/usr/bin/Rscript" tmp-fname)]
        (cond (not= 0 (:exit status))
              {:err (:err status)}
              (not (. (io/file ftype-fname) exists))
              {:err "Resulting file does not exist."}
              :else
              (do (update-graph-attachment doc ftype-fname graph ftype theme width height)
                  {:success true}))))))

(defn get-graph-png
  [doc graph]
  (render-graph-file doc graph "png" "minimal" 7 4)
  (if-let [f (get-attachment (:_id doc)
                             (format "%s-%s-%s-%s-%s-%s" (:_id graph) (:_rev graph) "png"
                                "minimal" 7 4))]
    (try (io/input-stream f) (catch Exception _))))

(defn get-graph-pdf
  [doc graph theme width height]
  (render-graph-file doc graph "pdf" theme width height)
  (if-let [f (get-attachment (:_id doc)
                             (format "%s-%s-%s-%s-%s-%s" (:_id graph) (:_rev graph) "pdf"
                                theme width height))]
    (try (io/input-stream f) (catch Exception _))))

(defn delete-graph
  [id]
  (delete-doc (get-doc id)))
