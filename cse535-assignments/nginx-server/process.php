<?php

	//ini_set('display_errors', 1);
	//ini_set('display_startup_errors', 1);
	//error_reporting(E_ALL);

    //echo "The ID is: " . $_GET["id"];
    //$out = shell_exec('python3 model.py');
    //var_dump($out);

	
	//ob_implicit_flush(true);ob_end_flush();

    //$cmd = "python3 model.py";

    $a = popen('python3 model.py', 'r'); 
    
    while($b = fgets($a, 2048)) { 
        echo $b."<br>\n"; 
        ob_flush();flush(); 
    }

    pclose($a);

?>
