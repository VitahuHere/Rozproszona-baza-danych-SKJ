Praca napisana przez Cong Minh Vu, s25206 11c


1. Kompilacja:

Standardowa kompilacja z użyciem komendy javac.

cd src
javac DatabaseClient.java DatabaseNode.java Node.java

2. Uruchomienie:
2.1 Uruchomienie węzła sieci:
java DatabaseNode -tcpport <port> -record <key>:<value> [-connect <host>:<port>]

gdzie args to:
-tcpport <port> określa numer portu TCP na którym dany węzeł sieci oczekuje na połączenia od klientów.

-record <klucz>:<wartość> oznacza parę liczb całkowitych początkowo przechowywanych w bazie na danym węźle,
gdzie pierwsza to klucz a druga to wartość związana z tym kluczem. Nie ma wymogu unikalności zarówno klucza jak i wartości.

[-connect <host>:<port>] oznacza listę innych węzłów już będących w sieci,
z którymi dany węzeł ma się połączyć i z którymi może się komunikować w celu wykonywania operacji.
Lista może zawierać wiele węzłów. Dla pierwszego węzła w sieci ta lista jest pusta.

Przykład:
java DatabaseNode -tcpport 1235 -record 1:2
java DatabaseNode -tcpport 1234 -record 1:2 -connect localhost:1235


2.2 Uruchomienie klienta:
java DatabaseClient -gateway <host>:<port> [args]

gdzie args to:
set-value <key>:<value>
get-value <key>
find-value <key>
get-max
get-min
new-record <key>:<value>
terminate

Przykład:
java DatabaseClient -gateway localhost:9991 -operation get-max


3. Opis działania:
3.1 Węzeł sieci:
Węzeł sieci jest inicjowany przez DatabaseNode, jednak cała logika komunikacji jest zawarta w klasie Node.
Komunikacja między węzłami odbywa się poprzez TCP. ServerSocket i Socket.
Node:
- dziedziczy po klasie Thread.
- posiada pola:
	private String address; - przechowuje host węzła.
	private String value; - przechowuje w wersji tekstowej klucz i wartość w postacji: key:value.
	private final ArrayList<String> nodes; - przechowuje listę adresów i portów węzłów do których jest połączony.
	private final ArrayList<String> visitedNodes; - przechowuje listę adresów i portów węzłów z którymi się komunikował w danym łańcuchu zapytań.
- posiada konstruktor:
	public Node(String address, String value, ArrayList<String> nodes) - inicjuje węzeł sieci.
- posiada metody:
	public void run() - nadpisuje metodę run() z klasy Thread.
						W tej metodzie uruchamia się ServerSocket i czeka na połączenie z klientem.
	setValue(String pair) - ustawia wartość węzła.
	getValue(String key) - zwraca wartość węzła.
	findKey(String key) - zwraca wartość związana z kluczem key.
	getMax() - zwraca maksymalną wartość z wszystkich węzłów.
	getMin() - zwraca minimalną wartość z wszystkich węzłów.
	newRecord(String record) - dodaje nowy węzeł do sieci. Nazwa zmiennej record odnosi się do pary klucz:wartość.
	terminate() - kończy działanie węzła.
	communicateWithNodes() - komunikuje się z połączonymi węzłami. Pomocnicza metoda do setValue(), getValue() i findKey().

3.2 Szczegóły metod:
3.2.0 run()
Uruchamia się ServerSocket i czeka na połączenie z klientem.
Po połączeniu z klientem uruchamia się BufferedReader i czeka na wiadomość od klienta.
Po odebraniu wiadomości wywołuje odpowiednią metodę w zależności od wiadomości.
Sprawdza poprawną ilość argumentów w wiadomości.

3.2.1 setValue(String pair)
Jeżeli klucz jest przechowywany w tym węźle, to ustawia wartość węzła na wartość związaną z kluczem.
W przeciwnym wypadku komunikuje się z połączonymi węzłami szukając klucza.
Zwraca "ERROR" jeżeli nie znaleziono klucza w żadnym węźle.
Nazwa zmiennej pair odnosi się do pary klucz:wartość.

3.2.2 getValue(String key)
Jeżeli klucz do wartości jest przechowywany w tym węźle to zwraca wartość związaną z kluczem.
W przeciwnym wypadku, komunikuje się z połączonymi węzłami szukając klucza.
Zwraca "ERROR" jeżeli nie znaleziono klucza w żadnym węźle.

3.2.3 findKey(String key)
Jeżeli klucz do wartości jest przechowywany w którymś z węzłów to zwraca hosta i port węzła,
w którym przechowywana jest wartość związana z kluczem.
W przeciwnym wypadku zwraca "ERROR" jeżeli nie znaleziono klucza w żadnym węźle.

3.2.4 getMax()
Zwraca maksymalną wartość ze wszystkich węzłów.
Polecenie zawsze będzie komunikować się z połączonymi węzłami, nawet jeżeli przechowywana wartość w tym węźle jest największa.
Ma to na celu sprawdzenie we wszystkich węzłach czy nie ma większej wartości.

3.2.5 getMin()
Zwraca minimalną wartość ze wszystkich węzłów.
Polecenie zawsze będzie komunikować się z połączonymi węzłami, nawet jeżeli przechowywana wartość w tym węźle jest najmniejsza.
Ma to na celu sprawdzenie we wszystkich węzłach czy nie ma mniejszej wartości.

3.2.6 newRecord(String record)
Ustala w węźle nową parę klucz:wartość.
Zwraca OK jeżeli udało się ustawić nową parę klucz:wartość.

3.2.7 terminate()
Kończy działanie węzła. Wysyła sygnał do wszystkich węzłów, żeby usunęły ten węzeł z listy połączonych węzłów.
Zamyka ServerSocket i kończy działanie wątku.

3.2.8 communicateWithNodes()
Komunikuje się z połączonymi węzłami.
Jednym z problemów była sytuacja w której węzeł wysyłał wiadomość do innego węzła, który wysłał wiadomość do tego węzła.
A -> B -> A
Pole typu ArrayList<String> o nazwie visited_nodes, ma na celu ustalić kto w łańcuchu zapytań, został już odwiedzony.
W ten sposób można uniknąć sytuacji zapętlania się komunikacji.
Inicjowanie komunikacji odbywa się poprzez wysłanie wiadomości "visited_nodes [node_strings]" do węzła, z którym chcemy się połączyć.
Węzeł, który otrzymał wiadomość, zapisuje lokalnie do własnej listy wiadomość "visited_nodes [node_strings]".
Sprawdzane jest czy węzeł z którym chce się się połączyć, znajduje się w liście visited_nodes,
jeżeli tak to nie wysyła żadnej wiadomości, jeżeli nie, to komunikacja przebiega normalnie.

4. Wielowątkowość
Wielowątkowość nie została zaimplementowana.
Ma to na celu uniknięcie sytuacji, w której węzeł jest w trakcie wykonywania operacji typu:
getMax(), getMin(), findKey() i w tym czasie otrzymuje wiadomość z poleceniem modyfikacji danych zawartych w węźle.

5. Testy
W teorii wszystkie funkcjonalności powinny działać poprawnie, z wyjątkiem wielowątkowości.
Skrypty testowe dostarczone do zadania wykonują się poprawnie, z wyjątkiem script-7-p.bat.