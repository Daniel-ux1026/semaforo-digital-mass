import '@angular/compiler';
import {setupTestBed} from '@analogjs/vitest-angular/setup-testbed';

setupTestBed({
  zoneless: true,
  errorOnUnknownElements: true,
  errorOnUnknownProperties: true,
  teardown: {destroyAfterEach: true}
});
