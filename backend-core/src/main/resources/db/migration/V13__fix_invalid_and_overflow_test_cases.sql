-- ============================================================================
-- V13: Fix invalid, non-unique, unreachable, non-BST, non-char, and 32-bit
--      int overflow test cases discovered during full 750-submission QA audit.
-- ============================================================================

-- 1. unique-paths: 5 cases whose outputs exceeded 32-bit signed int (> 2,147,483,647)
UPDATE test_cases SET input_data = 'm = 10, n = 12', expected_output = '167960' WHERE id = '845bb322-6bb7-435e-b25b-5dd819b1666c';
UPDATE test_cases SET input_data = 'm = 12, n = 10', expected_output = '167960' WHERE id = 'a657b100-d5db-4143-b13c-441e2f325584';
UPDATE test_cases SET input_data = 'm = 17, n = 17', expected_output = '601080390' WHERE id = '3bf8b48c-0a33-4bd2-ad19-8f571809d6dc';
UPDATE test_cases SET input_data = 'm = 19, n = 13', expected_output = '86493225' WHERE id = 'f7164312-3a95-48db-81db-27d9d51963eb';
UPDATE test_cases SET input_data = 'm = 16, n = 18', expected_output = '565722720' WHERE id = '49ce25ec-0fe2-4a3d-9180-5dce27f8f519';

-- 2. combination-sum-iv: 3 cases whose outputs exceeded 32-bit signed int (up to 600 digits!)
UPDATE test_cases SET input_data = 'nums = [1,5,10], target = 30', expected_output = '4229' WHERE id = '3d86a217-9a39-41ff-a6bb-a3de300c8fc0';
UPDATE test_cases SET input_data = 'nums = [1,2,3,4,5], target = 20', expected_output = '400096' WHERE id = 'bed521fa-e269-42af-9fcb-56bf0beb26bb';
UPDATE test_cases SET input_data = 'nums = [3,6,9], target = 30', expected_output = '274' WHERE id = '383d288c-dbb6-4889-88ba-4160b894ee92';

-- 3. coin-change-ii: 1 hidden case whose output (351,757,492,460) exceeded 32-bit signed int
UPDATE test_cases SET input_data = 'amount = 500, coins = [3,5,7,8,9,10]', expected_output = '4227159' WHERE id = 'fed12b5b-fa20-49ed-b925-4204f7f3f271';

-- 4. koko-eating-bananas: 1 case whose input h = 3,000,000,000 exceeded 32-bit int
UPDATE test_cases SET input_data = 'piles = [805306368,805306368,805306368], h = 1000000000', expected_output = '3' WHERE id = '3def7bdb-50b0-4bbe-9468-6f0439edb74d';

-- 5. 4sum: 2 cases whose input target = 4,000,000,000 exceeded 32-bit int
UPDATE test_cases SET input_data = 'nums = [1000000000,1000000000,1000000000,1000000000], target = -294967296', expected_output = '[]' WHERE id = '4efb3c95-edce-406a-b4eb-38b9a1a36f5b';
UPDATE test_cases SET input_data = 'nums = [500000000,500000000,500000000,500000000], target = 2000000000', expected_output = '[[500000000, 500000000, 500000000, 500000000]]' WHERE id = '18e812ae-8388-4f5e-9dfe-e441ba4583e9';

-- 6. product-of-array-except-self: 2 cases whose products exceeded 32-bit (and 64-bit!) int
UPDATE test_cases SET input_data = 'nums = [-5,-4,-3,-2,-1]', expected_output = '[24, 30, 40, 60, 120]' WHERE id = 'bf2d48c5-299f-4009-8184-ca5e115f65f5';
UPDATE test_cases SET input_data = 'nums = [1,2,3,4,5,6,7,8,9,10]', expected_output = '[3628800, 1814400, 1209600, 907200, 725760, 604800, 518400, 453600, 403200, 362880]' WHERE id = 'be05a7e8-a729-45d7-88e3-9e98bbd6f134';

-- 7. sqrtx: 3 cases whose input x exceeded 32-bit signed int (constraint: 0 <= x <= 2^31 - 1)
UPDATE test_cases SET input_data = 'x = 2147395599', expected_output = '46339' WHERE id = '963b295a-4de2-4373-92cb-d774defc5145';
UPDATE test_cases SET input_data = 'x = 2147395600', expected_output = '46340' WHERE id = '4beb48ed-c993-43be-8765-4d0a2d289513';
UPDATE test_cases SET input_data = 'x = 1000000000', expected_output = '31622' WHERE id = '56be0c6d-5605-45a2-a804-924ffbfbf52a';

-- 8. number-of-1-bits: 1 case whose input n = 2^40 exceeded 32-bit unsigned int
UPDATE test_cases SET input_data = 'n = 1073741824', expected_output = '1' WHERE id = 'c2b89ba3-f9a9-4a3b-80c9-8404eb92438a';

-- 9. find-minimum-in-rotated-sorted-array: 3 invalid non-rotated arrays (and wrong expected output)
UPDATE test_cases SET input_data = 'nums = [2,3,4,5,6,7,8,9,0,1]', expected_output = '0' WHERE id = 'bb1a0b91-c4fc-43b3-87ad-400c275ed224';
UPDATE test_cases SET input_data = 'nums = [2,3,1]', expected_output = '1' WHERE id = '9e51edaf-56f5-4016-a938-efafec527535';
UPDATE test_cases SET input_data = 'nums = [4,5,1,2,3]', expected_output = '1' WHERE id = '45030570-a74a-4747-822c-0ffe9624f2a4';

-- 10. search-in-rotated-sorted-array: 1 non-rotated array with wrong expected output (-1 when target=1 was present)
UPDATE test_cases SET input_data = 'nums = [4,5,6,7,0,1,2], target = 1', expected_output = '5' WHERE id = 'd9e946da-6cd3-4ae2-af75-36fb1083d630';

-- 11. gas-station: 7 cases violating the "guaranteed unique solution" constraint
UPDATE test_cases SET input_data = 'gas = [1,2,3,4], cost = [2,3,4,1]', expected_output = '3' WHERE id = 'b1234f1f-fb59-4014-a75b-80708a65b985';
UPDATE test_cases SET input_data = 'gas = [1,1,1,1,6], cost = [2,2,2,2,2]', expected_output = '4' WHERE id = '7411f7ed-030f-49a1-ac94-4352e60b8c2b';
UPDATE test_cases SET input_data = 'gas = [5,0,0,0,0], cost = [1,1,1,1,1]', expected_output = '0' WHERE id = 'c19e60dc-6fff-4302-b9ee-93f52953d5f5';
UPDATE test_cases SET input_data = 'gas = [2,1,2,3,7], cost = [3,4,5,1,2]', expected_output = '3' WHERE id = '4bf597c2-ae7f-4bce-8fef-f01c7259b9f9';
UPDATE test_cases SET input_data = 'gas = [1,2,3,4,5,6], cost = [2,3,4,5,6,1]', expected_output = '5' WHERE id = '70b6c0d4-4be6-43a7-ac7b-d6d37deab88e';
UPDATE test_cases SET input_data = 'gas = [1,1,5,1,1], cost = [2,2,1,2,2]', expected_output = '2' WHERE id = '932cad11-50af-4234-85e6-c3ae02cfe85d';
UPDATE test_cases SET input_data = 'gas = [2,3,4,5,1], cost = [3,4,5,1,2]', expected_output = '3' WHERE id = '2dbe5891-5d44-4aca-8a52-1e47a453c7c8';

-- 12. jump-game-ii: 2 unreachable arrays causing infinite loop / TLE in standard O(n) greedy BFS
UPDATE test_cases SET input_data = 'nums = [2,3,1,1,4,2,1,1,1,1]', expected_output = '4' WHERE id = '79d8a89a-2c0c-44d8-aa0d-8a0457dcb229';
UPDATE test_cases SET input_data = 'nums = [3,2,1,1,4,2,1,2,1,1]', expected_output = '4' WHERE id = '86632f23-77df-4dfb-bef6-9ef6d5a99363';

-- 13. reverse-string: 1 case containing multi-character strings ["Python","!","is","fun"] instead of char[]
UPDATE test_cases SET input_data = 's = ["P","y","t","h","o","n","!"]', expected_output = '["!", "n", "o", "h", "t", "y", "P"]' WHERE id = '320437e0-c232-4637-96d6-70234feba751';

-- 14. swim-in-rising-water: 2 non-permutation grids (and wrong expected=8 instead of 5 on case #7)
UPDATE test_cases SET input_data = 'grid = [[0,1,2],[5,4,3],[6,7,8]]', expected_output = '8' WHERE id = '34114c4b-20ff-4f17-81bd-f07eb09c8d1e';
UPDATE test_cases SET input_data = 'grid = [[0,1,4,5],[2,3,6,7],[8,9,10,11],[12,13,14,15]]', expected_output = '15' WHERE id = '43f69e7f-8414-4f63-8f9f-ac1923247a8c';

-- 15. delete-node-in-a-bst: 3 invalid non-BST input trees
UPDATE test_cases SET input_data = 'root = [2,1,null], key = 1', expected_output = '[2]' WHERE id = 'b40a1481-1261-4223-a068-2732d335f58c';
UPDATE test_cases SET input_data = 'root = [8,4,12,2,6,10,14,1,3,5,7,9,11,13,15], key = 8', expected_output = '[9, 4, 12, 2, 6, 10, 14, 1, 3, 5, 7, null, 11, 13, 15]' WHERE id = 'd94b0057-bd95-4f00-9649-2a8a7f6e3645';
UPDATE test_cases SET input_data = 'root = [50,30,70,20,40,60,80], key = 60', expected_output = '[50, 30, 70, 20, 40, null, 80]' WHERE id = '8bddcd38-ad40-4819-89bc-6dd1c950bbef';
