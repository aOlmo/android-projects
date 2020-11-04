<?php
    echo $_GET["id"];
    echo getcwd() . "\n";
    $output = shell_exec('./model.py');
    var_dump($output);
?>
