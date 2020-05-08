(def dim 40)
;number of ants = nants-sqrt^2
(def nants-sqrt 7)
;number of places with food
(def food-places 35)
;range of amount of food at a place
(def food-range 100)
;scale factor for pheromone drawing
(def pher-scale 20.0)
;scale factor for food drawing
(def food-scale 30.0)
;evaporation rate
(def evap-rate 0.010)


(def animation-sleep-ms 10)
(def ant-sleep-ms 400)
(def evap-sleep-ms 100)

(def running true)

(defstruct cell :food :pher) ;may also have :ant and :home

;world is a 2d vector of refs to cells
(def world
     (apply vector
            (map (fn [_]
                   (apply vector (map (fn [_] (ref (struct cell 0 0)))
                                      (range dim))))
                 (range dim))))

(defn place [[x y]]
  (-> world (nth x) (nth y)))

(defstruct ant :dir :infect)

(defn create-ant
  "create an ant at the location, returning an ant agent on the location"
  [loc dir infect]
    (sync nil
      (let [p (place loc)
            a (struct ant dir infect)]
        (alter p assoc :ant a)
        (agent loc))))

(defn bound
  "returns n wrapped into range 0-b"
  [b n]
    (let [n (rem n b)]
      (if (neg? n)
        (+ n b)
        n)))

(def dir-delta {0 [0 -1]
                1 [1 -1]
                2 [1 0]
                3 [1 1]
                4 [0 1]
                5 [-1 1]
                6 [-1 0]
                7 [-1 -1]})

(defn delta-loc
  "returns the location one step in the given dir. Note the world is a torus"
  [[x y] dir]
    (let [[dx dy] (dir-delta (bound 8 dir))]
      [(bound dim (+ x dx)) (bound dim (+ y dy))]))

(defn turn
  "turns the ant at the location by the given amount"
  [loc amt]
    (dosync
     (let [p (place loc)
           ant (:ant @p)]
       (alter p assoc :ant (assoc ant :dir (bound 8 (+ (:dir ant) amt))))))
    loc)

(defn move
  "moves the ant in the direction it is heading. Must be called in a
  transaction that has verified the way is clear"
  [loc]
     (let [oldp (place loc)
           ant (:ant @oldp)
           newloc (delta-loc loc (:dir ant))
           p (place newloc)]
         ;move the ant
       (alter p assoc :ant ant)
       (alter oldp dissoc :ant)
         ;leave pheromone trail
       newloc))

(defn behave [loc]
	(let [p (place loc)
		ant (:ant @p)
        ahead (place (delta-loc loc (:dir ant)))
        ahead-left (place (delta-loc loc (dec (:dir ant))))
        ahead-right (place (delta-loc loc (inc (:dir ant))))
		places [ahead ahead-left ahead-right]]
	(. Thread (sleep  ant-sleep-ms))
	(dosync
	(when running
		(send-off *agent* #'behave))
	(if (and (= (:infect (:ant @p)) 0) ( or (and (:ant @ahead) (>= (:infect (:ant @ahead)) 1)) ( and (:ant @ahead-left) (>= (:infect (:ant @ahead-left)) 1)) (and (:ant @ahead-right) (>= (:infect (:ant @ahead-right)) 1))))
	(do
		(alter p dissoc :ant)
		(alter p assoc :ant ( assoc ant :infect ( inc (:infect ant)))))
	(if (and (:ant @p) (>= (:infect (:ant @p)) 1))
	(do
		(alter p dissoc :ant)
		(alter p assoc :ant ( assoc ant :infect (+ evap-rate (:infect ant)))))))

	(if (not (:ant @ahead))
		(move loc)
	(-> loc (turn (rand-int 4)))))))


(def home-off (/ dim 4))
(def home-range (range home-off (+ nants-sqrt home-off)))


(defn setup
  "places initial food and ants, returns seq of ant agents"
  []
  (sync nil
    (doall
	(conj
		(for [x home-range y  home-range]
		(do
		(let [ x (+ (rand-int dim) 0)
			   y (+ (rand-int dim) 0)]

			(create-ant [x  y] (rand-int 7) 0))))
			(create-ant [20  20] (rand-int 7) 1)))))

(defn err-handler-fn [ag ex]
  (println " " ex "value " @ag))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(import
 '(java.awt Color Graphics Dimension)
 '(java.awt.image BufferedImage)
 '(javax.swing JPanel JFrame))

;pixels per world cell
(def scale 10)

(defn fill-cell [#^Graphics g x y c]
  (doto g
    (.setColor c)
    (.fillRect (* x scale) (* y scale) scale scale)))


(defn render-ant [ant #^Graphics g x y]
  (let [black (. (new Color 0 0 0 255) (getRGB))
        pink (. (new Color 255 192 203 255) (getRGB))
        red (. (new Color 255 0 0 255) (getRGB))
        [hx hy tx ty] ({0 [2 0 2 4]
                        1 [4 0 0 4]
                        2 [4 2 0 2]
                        3 [4 4 0 0]
                        4 [2 4 2 0]
                        5 [0 4 4 0]
                        6 [0 2 4 2]
                        7 [0 0 4 4]}
                       (:dir ant))]
    (doto g
      (.setColor (cond
( and (< (:infect ant) 2) (>= (:infect ant) 1)) (new Color 255 0 0 255)
                    ( = (:infect ant) 0)  (new Color 255 255 255 255)
( >= (:infect ant) 2)
  (new Color 0 255 0 255)))
      (.drawLine (+ hx (* x scale)) (+ hy (* y scale))
                (+ tx (* x scale)) (+ ty (* y scale))))))

(defn render-place [g p x y]
(when (:ant p)
    (render-ant (:ant p) g x y)))

(defn render [g]
  (let [v (dosync (apply vector (for [x (range dim) y (range dim)]
                                   @(place [x y]))))
        img (new BufferedImage (* scale dim) (* scale dim)
                 (. BufferedImage TYPE_INT_ARGB))
        bg (. img (getGraphics))]
    (doto bg
      (.setColor (. Color black))
      (.fillRect 0 0 (. img (getWidth)) (. img (getHeight))))
    (dorun
     (for [x (range dim) y (range dim)]
       (render-place bg (v (+ (* x dim) y)) x y)))

    (. g (drawImage img 0 0 nil))
    (. bg (dispose))))

(def panel (doto (proxy [JPanel] []
                        (paint [g] (render g)))
             (.setPreferredSize (new Dimension
                                     (* scale dim)
                                     (* scale dim)))))

(def frame (doto (new JFrame) (.add panel) .pack .show))

(def animator (agent nil))

(defn animation [x]
  (when running
    (send-off *agent* #'animation))
  (. panel (repaint))
  (. Thread (sleep animation-sleep-ms))
  nil)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn write-file [msg]
(with-open [w (clojure.java.io/writer  "C:/Users/ashup/data.csv" :append true)]
(.write w msg)))

(write-file "well,recovered\n")
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ants (setup))

(defn vect []
 (map (comp :infect :ant deref place deref) ants))

(defn evaporate []
  "causes all the pheromones to evaporate a bit"
	(def aa (vect))
	(def recovered  (count (filter #( >= % 2)(filter identity aa))))
	(def well (count (filter #( = % 0)(filter identity aa))))
	(def infected (count (filter #(< % 2) (filter #( >= % 1) (filter identity aa)))))
	(write-file (str well "," recovered "\n") )
)

(send-off animator animation)
(dorun (map #(send % behave) ants))

(defn print-count []
(while true (do 
    (evaporate)
	(. Thread (sleep evap-sleep-ms )))))

(print-count)

;;(def a (count (filter pos? (map (comp :infect :ant deref place deref) ants))))


;;(map #(set-error-handler! % err-handler-fn) ants)
;;(def corona (create-ant [20 20] 1 1))
;;(conj ants corona)
