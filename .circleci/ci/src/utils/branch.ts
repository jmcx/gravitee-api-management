export const supportBranchPattern = '\\d+\\.\\d+\\.x';

export function sanitizeBranch(branch: string) {
  return branch
    .replaceAll(/[~^]+/g, '')
    .replaceAll(/[^a-zA-Z0-9\\.]+/g, '-')
    .replaceAll(/^-+|-+$/g, '')
    .toLowerCase()
    .substring(0, 60);
}

export function isE2EBranch(branch: string): boolean {
  const regex = new RegExp('.*-run-e2e.*');
  return regex.test(branch);
}

export function isMasterBranch(branch: string) {
  return branch === 'master';
}

export function isSupportBranch(branch: string): boolean {
  const regex = new RegExp(supportBranchPattern);
  return regex.test(branch);
}

export function isSupportBranchOrMaster(branch: string): boolean {
  const branchPattern = new RegExp(`^(${supportBranchPattern})|master$`);
  return branchPattern.test(branch);
}
