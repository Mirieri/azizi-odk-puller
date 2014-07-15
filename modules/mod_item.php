<?php
   class ProcessItemSets {

   private $isDir;//directory where the itemsets are defined
   private $itemSets;

   public function _construct($isDir) {
      $this->isDir = $isDir;
      
      //get all the json files in $isDir
      $jsonFiles = $this->getAllJSONFiles($this->isDir);
      
      //try initializing arrays for all the json files
      
   }

   private function getAllJSONFiles($dir){
      $fileNames = array();
      if($handler = opendir($dir)){
         while(false !== ($file = readdir($handler))){
            if ($file !== "." && $file !== ".." && strtolower(substr($file, strrpos($file, '.') + 1)) == "json"){
               array_push($fileNames, $file);
            }
         }
      }
      
      return $fileName;
   }

   private function initJsonFile($fileName){
      $jsonString = file_get_contents($fileName);

      $jsonArray = json_decode($jsonString, true);
      
      return $jsonArray;
   }
   
   private function getQuery($item){
      
   }
} 
?>
