(ns ^:dev/once votingbuddy.app
  (:require
   [devtools.core :as devtools]
   [votingbuddy.core :as core]))

(enable-console-print!)

(println "loading env/dev/cljs/votingbuddy/app.cljs...")

(devtools/install!)

(core/init!)