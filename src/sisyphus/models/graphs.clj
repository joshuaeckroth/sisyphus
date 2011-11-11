(ns sisyphus.models.graphs
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.results :only [csv-filenames results-to-csv]])
  (:use sisyphus.models.common))

(defn png-filename
  [run graph]
  (format "%s/%s-%s-%s.png" cachedir
          (:_id run) (:_id graph) (:_rev graph)))

(defn list-graphs
  []
  (let [all-graphs (:rows (view "graphs-list"))
        problems (set (map (comp first :key) all-graphs))]
    (reduce (fn [m problem] (assoc m problem
                                   (map :value (filter (fn [g] (= problem (first (:key g))))
                                                       all-graphs))))
            {} problems)))

(defn get-graph
  [problem n]
  (:value (first (:rows (view "graphs-list" {:key [problem n]})))))

(defn new-graph
  [graph]
  (create-doc (assoc graph :type "graph")))

(defn update-graph
  [graph]
  (clutch/with-db db
    (clutch/update-document (get-doc (:id graph)) (dissoc graph :id :_id :_rev))))

(defn update-graph-attachment
  [runid png-fname graph]
  (try
    (clutch/with-db db
      (clutch/update-attachment (get-doc runid) png-fname
                                (format "%s-%s" (:_id graph) (:_rev graph))
                                "image/png"))
    (catch Exception e (update-graph-attachment runid png-fname graph))))

(defn get-graph-png
  [runid graphid graphrev]
  (if-let [png (get-attachment runid (format "%s-%s" graphid graphrev))]
    png
    (let [run (get-doc runid)
          graph (get-doc graphid graphrev)
          csv-fnames (csv-filenames run)
          png-fname (png-filename run graph)
          tmp-fname (format "%s/%s-%s-%s.rscript"
                            cachedir runid (:_id graph) (:_rev graph))
          rcode (format "library(ggplot2)\n%s\n%s\nggsave(\"%s\", plot = p, dpi = 100, width = 7, height = 4)"
                        (apply str (map #(format "%s <- read.csv(\"%s\")\n" (name %) (get csv-fnames %))
                                        (keys csv-fnames)))
                        (:code graph) png-fname)]
      (results-to-csv run csv-fnames)
      ;; save rcode to file
      (with-open [writer (io/writer tmp-fname)]
        (.write writer rcode))
      ;; run Rscript
      (let [status (sh "/usr/bin/Rscript" tmp-fname)]
        (cond (not= 0 (:exit status))
              status
              (not (. (io/file png-fname) exists))
              {:err "Resulting file does not exist."}
              :else (do (update-graph-attachment runid png-fname graph)
                        (io/input-stream (io/file png-fname))))))))
