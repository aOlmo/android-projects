<?php

	//ini_set('display_errors', 1);
	//ini_set('display_startup_errors', 1);
	//error_reporting(E_ALL);
	
	//print_r($_POST);

	$date = str_replace("-", "", $_POST["date"]);
		
	$cmd = 'python3 model.py --id '.$_POST['id']." --date ".$date;
    $a = popen($cmd, 'r'); 
    
    while($b = fgets($a, 2048)) { 
        echo $b."<br>\n"; 
        ob_flush();flush(); 
    }

    pclose($a);

?>
