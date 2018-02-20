(ns certo.models-default-test
  (:require [clojure.test :refer :all]
            [certo.models.default :refer :all]))

(deftest insert
  (is (= 4 (+ 2 2)))
  (is (= 7 (+ 3 4))))
