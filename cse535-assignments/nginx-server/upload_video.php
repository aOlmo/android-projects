<?php 

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

echo 'Here is some more debugging info:\n';
print_r($_FILES);
print_r($_POST);

echo getcwd() . "\n";


$file_path = "/var/www/html/cse565-a2/uploads/";

$file = $file_path . basename($_FILES['uploaded_file']['name']);

echo $file;

if (move_uploaded_file($_FILES['uploaded_file']['tmp_name'], $file)) {
    echo "Uploaded";

} else {
  echo "Failed\n";

}

?>
