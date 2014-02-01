(ns hs2.core-test
  (:require [clojure.test :refer :all]
            [hs2.core :refer :all]))

; (deftest a-test
;   (testing "FIXME, I fail."
    ; (is (= 0 1))))
(deftest hex2int-test
	(testing "hex2int-test: zero"
		(is (= 0 (hex2int "0x00000000"))))
	(testing "hex2int-test: five"
		(is (= 5 (hex2int "0x00000005"))))
	(testing "hex2int-test: -five"
		(is (= -5 (hex2int "0xFFFFFFFB"))))
	(testing "hex2int-test: -2056"
		(is (= -2056 (hex2int "0xfffff7f8"))))
)
       

