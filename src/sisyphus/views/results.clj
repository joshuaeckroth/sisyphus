(ns sisyphus.views.results
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:require [noir.cookies :as cookies]))

(defpartial field-checkbox
  [run n field]
  [:li [:label
        [:input {:type "checkbox" :name (format "%s[]" (name n)) :value (name field)
                 :checked (= "true" (cookies/get (format "%s-%s" (:problem run) (name field))))}]
        " " (name field)]])

(defpartial field-checkboxes
  [run n fields]
  (let [field-groups (partition-all (int (Math/ceil (/ (count fields) 3))) fields)]
    (map (fn [fs]
           [:div.span4.columns
            [:ul.inputs-list (map (fn [f] (field-checkbox run n f)) fs)]])
         field-groups)))

(defpartial comparative-results-table
  [comparative-results on-fields]
  [:div.row
   [:div.span16.columns {:style "max-width: 960px; max-height: 20em; overflow: auto;"}
    [:table.tablesorter.zebra-striped
     [:thead
      [:tr (map (fn [f] [:th (name f)]) on-fields)]]
     [:tbody
      (map (fn [r] [:tr (map (fn [f] [:td (let [val (get r f)]
                                            (if (= java.lang.Double (type val))
                                              (format "%.2f" val)
                                              (str val)))])
                             on-fields)])
           comparative-results)]]]])

(defpartial paired-results-table
  [control-results comparison-results on-fields]
  (let [results-map (reduce (fn [m r] (update-in m [(:Seed r)] conj r))
                            (zipmap (map :Seed control-results)
                                    (map (fn [r] [r]) control-results))
                            comparison-results)]
    [:div.row
     [:div.span16.columns {:style "max-width: 960px; max-height: 20em; overflow: auto;"}
      [:table.tablesorter.zebra-striped
       [:thead
        [:tr (map (fn [f] [:th (name f)]) on-fields)]]
       [:tbody
        (map (fn [s]
               [:tr (map (fn [f]
                           [:td (let [control-val (get (first (get results-map s)) f)
                                      comparison-val (get (second (get results-map s)) f)]
                                  (if (not= control-val comparison-val)
                                    (if (and (= java.lang.Double (type control-val))
                                             (= java.lang.Double (type comparison-val)))
                                      (format "<strong>%.2f</strong><br/>%.2f"
                                              comparison-val control-val)
                                      (format "<strong>%s</strong><br/>%s"
                                              (str comparison-val) (str control-val)))
                                    (if (= java.lang.Double (type control-val))
                                      (format "%.2f" control-val)
                                      (str control-val))))])
                         on-fields)])
             (filter (fn [s] (= 2 (count (get results-map s))))
                     (sort (keys results-map))))]]]]))
