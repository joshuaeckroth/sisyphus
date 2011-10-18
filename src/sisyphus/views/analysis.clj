(ns sisyphus.views.analysis
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.analysis :only [list-analysis new-analysis update-analysis get-analysis-output]])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial show-analysis
  [run analysis]
  [:div.row
   [:div.span4.columns
    [:h3 (:name analysis)]
    [:p (:caption analysis)]]
   [:div.span12.columns
    [:pre (get-analysis-output (:_id run) (:_id analysis) (:_rev analysis))]]])

(defpartial analysis-form
  [analysis]
  [:section#analysis-form
   [:div.page-header
    [:h1 (if (:name analysis) (format "Update analysis %s" (:name analysis))
             "New analysis")]]
   [:div.row
    [:div.span4.columns
     [:h2 "Metadata"]]
    [:div.span12.columns
     (form-to [:post (if (:name analysis) "/analysis/update-analysis" "/analysis/new-analysis")]
              (hidden-field :id (:_id analysis))
              [:fieldset
               [:legend "Metadata"]
               [:div.clearfix
                [:label {:for "problem"} "Problem"]
                [:div.input
                 [:input.xlarge {:id "problem" :name "problem" :size 30
                                 :type "text" :value (:problem analysis)}]]]
               [:div.clearfix
                [:label {:for "name"} "Name"]
                [:div.input
                 [:input.xlarge {:id "name" :name "name" :size 30
                                 :type "text" :value (:name analysis)}]]]
               [:div.clearfix
                [:label {:for "caption"} "Caption"]
                [:div.input
                 [:textarea.xxlarge {:id "caption" :name "caption"} (:caption analysis)]]]
               [:div.clearfix
                [:label {:for "code"} "R code"]
                [:div.input
                 [:textarea.xxlarge {:id "code" :name "code"}
                  (if (:code analysis) (:code analysis)
                      "xyz")]
                 [:span.help-block "Assume the existence of data tables named 'control',
                                    'comparison', and 'comparative'."]]]
               [:div.actions
                [:input.btn.primary {:value (if (:name analysis) "Update" "Save") :type "submit"}]]])]]])

(defpage
  [:post "/analysis/update-analysis"] {:as analysis}
  (update-analysis analysis)
  (resp/redirect "/analysis"))

(defpage
  [:post "/analysis/new-analysis"] {:as analysis}
  (new-analysis analysis)
  (resp/redirect "/analysis"))

(defpage "/analysis/update/:id" {id :id}
  (let [analysis (get-doc id)]
    (common/layout
     (format "Update analysis %s" (:name analysis))
     (analysis-form analysis))))

(defpage "/analysis" {}
  (let [analysis (list-analysis)]
    (common/layout
     "Analysis"
     (for [problem (sort (keys analysis))]
       [:section {:id problem}
        [:div.row
         [:div.span16.columns
          [:div.page-header [:h1 (format "%s analysis" problem)]]]]
        (for [analysis (get analysis problem)]
          [:div.row
           [:div.span4.columns
            [:h2 (:name analysis)]
            [:p (:caption analysis)]
            [:p (link-to (format "/analysis/update/%s" (:_id analysis)) "Update analysis")]]
           [:div.span12.columns
            [:pre (:code analysis)]]])])
     (analysis-form {}))))